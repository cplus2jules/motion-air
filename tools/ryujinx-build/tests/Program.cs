using System.Reflection;
using System.Text.Json;
using System.Numerics;
using Ryujinx.Common.Configuration.Hid;
using Ryujinx.Common.Configuration.Hid.Keyboard;
using Ryujinx.Common.Configuration.Hid.Controller.Motion;
using Ryujinx.Common.Utilities;
using Ryujinx.Input;
using Ryujinx.Input.HLE;
using Client = Ryujinx.Input.Motion.CemuHook.Client;
using KeyboardModel = Ryujinx.Ava.UI.Models.Input.KeyboardInputConfig;

static void Check(bool condition, string label) { if (!condition) throw new Exception(label); Console.WriteLine("PASS " + label); }
var options = JsonHelper.GetDefaultSerializerOptions();
var profile = (StandardKeyboardInputConfig)JsonSerializer.Deserialize<InputConfig>(File.ReadAllText(args[0]), options)!;
Check(profile.Motion is CemuHookMotionConfigController { EnableMotion: true, Slot: 0 }, "keyboard motion deserializes through real Ryujinx converter");
var saved = new KeyboardModel(profile).GetConfig();
var roundTrip = (StandardKeyboardInputConfig)JsonSerializer.Deserialize<InputConfig>(JsonSerializer.Serialize(saved, options), options)!;
Check(roundTrip.Motion is CemuHookMotionConfigController { EnableMotion: true, Slot: 0 }, "settings UI round-trip preserves keyboard motion");
var driver = new EmptyDriver();
var manager = new NpadManager(driver,driver,driver);
typeof(NpadManager).GetField("_inputConfig", BindingFlags.Instance|BindingFlags.NonPublic)!.SetValue(manager, new List<InputConfig>{profile});
using var client = new Client(manager);
var controller = new NpadController(client);
controller.UpdateUserConfiguration(profile);
client.HandleResponse(File.ReadAllBytes(args[1]),0);
client.HandleResponse(File.ReadAllBytes(args[2]),0);
Check(client.TryGetData(0,0,out var motion), "CemuHook accepts keyboard-backed motion");
Check(Vector3.Distance(motion.Accelerometer,new Vector3(0.25f,-0.75f,-0.5f)) < 0.0001f, "Npad acceleration matches phone device frame");
Check(Vector3.Distance(motion.Gyroscrope,new Vector3(90,-30,60)) < 0.0001f, "gyro sign convention matches pinned consumer");
client.HandleResponse(File.ReadAllBytes(args[1]),0);
Check(motion.TimeStamp == 1016666, "older UDP sample does not roll back sensor time");
typeof(NpadController).GetField("_leftMotionInput",BindingFlags.Instance|BindingFlags.NonPublic)!.SetValue(controller,motion);
var state = controller.GetHLEMotionState();
Check(state.Gyroscope.Length()>0 && state.Accelerometer.Length()>0,"single right Joy-Con supplies six-axis state");
for(int i=0;i<120;i++) controller.UpdateUserConfiguration(roundTrip);
Check(client.TryGetData(0,0,out _),"unchanged per-frame configuration retains motion");
((CemuHookMotionConfigController)roundTrip.Motion).DsuServerPort++;
controller.UpdateUserConfiguration(roundTrip);
Check(!client.TryGetData(0,0,out _),"in-place endpoint edit clears cached motion");
roundTrip.Motion.EnableMotion=false;
controller.UpdateUserConfiguration(roundTrip);
Check(controller.GetHLEMotionState().Gyroscope==Vector3.Zero,"motion disable clears six-axis input");
roundTrip.Motion=null;
controller.UpdateUserConfiguration(roundTrip);
Check(controller.GetHLEMotionState().Gyroscope==Vector3.Zero,"motion removal remains safe");
// Exercise the actual NpadController update, with a desktop keyboard that
// deliberately holds Plus. DSU player snapshots must replace those keys.
var fixtureDir = Path.GetDirectoryName(args[0])!;
var profiles = JsonSerializer.Deserialize<List<InputConfig>>(File.ReadAllText(Path.Combine(fixtureDir, "players.json")), options)!;
Check(profiles.Select(p => p.PlayerIndex).Distinct().Count() == 6 && profiles.All(p => p.ControllerType == ControllerType.JoyconRight), "six distinct player indices each use one right Joy-Con");
var sink = new System.Net.Sockets.UdpClient(new System.Net.IPEndPoint(System.Net.IPAddress.Loopback, 0));
using (sink)
{
    var keyboardDriver = new HeldKeyboardDriver();
    var multiplayerManager = new NpadManager(keyboardDriver, keyboardDriver, keyboardDriver);
    typeof(NpadManager).GetField("_inputConfig", BindingFlags.Instance|BindingFlags.NonPublic)!.SetValue(multiplayerManager, profiles);
    using var multiplayerClient = new Client(multiplayerManager);
    var controllers = new List<NpadController>();
    var expected = new[] { GamepadButtonInputId.A, GamepadButtonInputId.B, GamepadButtonInputId.X, GamepadButtonInputId.Y, GamepadButtonInputId.SingleLeftTrigger1, GamepadButtonInputId.SingleRightTrigger1 };
    for (int i = 0; i < 6; i++)
    {
        var config = (StandardKeyboardInputConfig)profiles[i];
        var cemu = (CemuHookMotionConfigController)config.Motion;
        Check(cemu.UseControllerInput && cemu.Slot == i % 4 && cemu.DsuServerPort == 26760 + i / 4, $"player {i + 1} has an independent endpoint/slot");
        var uiSaved = new KeyboardModel(config).GetConfig();
        Check(((CemuHookMotionConfigController)((StandardKeyboardInputConfig)uiSaved).Motion).UseControllerInput, "settings retain DSU controller input");
        cemu.DsuServerPort = ((System.Net.IPEndPoint)sink.Client.LocalEndPoint!).Port;
        var pad = new NpadController(multiplayerClient);
        pad.UpdateDriverConfiguration(keyboardDriver, config);
        pad.UpdateUserConfiguration(config);
        controllers.Add(pad);
        multiplayerClient.HandleResponse(File.ReadAllBytes(Path.Combine(fixtureDir, $"warmup-{i}.bin")), i);
        multiplayerClient.HandleResponse(File.ReadAllBytes(Path.Combine(fixtureDir, $"player-{i}.bin")), i);
        pad.Update();
        Check(pad.State.IsPressed(expected[i]) && !pad.State.IsPressed(GamepadButtonInputId.Plus), $"player {i + 1} receives its own button without desktop cross-talk");
        Check(pad.GetHLEInputState().Buttons != 0 && pad.GetHLEInputState().RStick.Dx > 0, $"player {i + 1} reaches game-facing buttons and stick");
        Check(Math.Abs(pad.GetHLEMotionState().Accelerometer.X - (i + 1) / 10f) < 0.002f, $"player {i + 1} reaches game-facing six-axis input");
    }
    for (int i = 0; i < 6; i++)
    {
        var released = File.ReadAllBytes(Path.Combine(fixtureDir, $"release-{i}.bin"));
        multiplayerClient.HandleResponse(released, i);
        // Older pressed packet must not undo a release, even with the same sensor timestamp.
        multiplayerClient.HandleResponse(File.ReadAllBytes(Path.Combine(fixtureDir, $"player-{i}.bin")), i);
        controllers[i].Update();
        Check(controllers[i].GetHLEInputState().Buttons == 0, $"player {i + 1} releases independently without fresh motion");
    }
    Thread.Sleep(300);
    Check(!multiplayerClient.TryGetControllerState(0, 0, out var stale) && !stale.IsPressed(GamepadButtonInputId.A), "stalled UDP input expires to neutral after 250 ms");
    foreach (var pad in controllers) pad.Dispose();
}
Console.WriteLine("Emulator motion contract passed. Physical game scoring remains untested.");
class EmptyDriver : IGamepadDriver {
 public string DriverName=>"Test"; public ReadOnlySpan<string> GamepadsIds=>Array.Empty<string>();
 public event Action<string> OnGamepadConnected {add{} remove{}} public event Action<string> OnGamepadDisconnected {add{} remove{}}
 public IGamepad GetGamepad(string id)=>null; public IEnumerable<IGamepad> GetGamepads()=>Array.Empty<IGamepad>(); public void Dispose(){}
}

class HeldKeyboardDriver : IGamepadDriver {
 public string DriverName => "Test keyboard"; public ReadOnlySpan<string> GamepadsIds => new[] { "0" };
 public event Action<string> OnGamepadConnected { add {} remove {} } public event Action<string> OnGamepadDisconnected { add {} remove {} }
 public IGamepad GetGamepad(string id) => new HeldKeyboard(); public IEnumerable<IGamepad> GetGamepads() => new[] { new HeldKeyboard() }; public void Dispose() {}
}
class HeldKeyboard : IKeyboard {
 public GamepadFeaturesFlag Features => default; public string Id => "0"; public string Name => "Held keyboard"; public bool IsConnected => true;
 public bool IsPressed(GamepadButtonInputId id) => id == GamepadButtonInputId.Plus;
 public bool IsPressed(Ryujinx.Input.Key key) => false; public KeyboardStateSnapshot GetKeyboardStateSnapshot() => default;
 public (float, float) GetStick(StickInputId id) => (0, 0); public Vector3 GetMotionData(MotionInputId id) => default;
 public void SetTriggerThreshold(float threshold) {} public void SetConfiguration(InputConfig config) {} public void SetLed(uint rgb) {}
 public void Rumble(float low, float high, uint duration) {} public void Dispose() {}
 public GamepadStateSnapshot GetMappedStateSnapshot() { var state = new GamepadStateSnapshot(); state.SetPressed(GamepadButtonInputId.Plus, true); return state; }
 public GamepadStateSnapshot GetStateSnapshot() => GetMappedStateSnapshot();
}

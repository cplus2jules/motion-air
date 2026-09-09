"""Plot samples exported by main.swift and the app's shared motion tokens."""
import csv
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

root = Path(__file__).resolve().parents[2]
rows = list(csv.DictReader((root / "docs/design/onboarding-motion.csv").open()))
data = {key: [float(row[key]) for row in rows] for key in rows[0]}
plt.rcParams.update({"font.family": "DejaVu Sans", "axes.spines.top": False,
                     "axes.spines.right": False, "font.size": 11})
fig, axes = plt.subplots(2, 2, figsize=(12, 7), layout="constrained")
fig.suptitle("Motion Air · motion that settles", fontsize=22, weight="bold", x=.04, ha="left")
for col, (prefix, title, color) in enumerate([
    ("page", "Page transition · 240 ms ease-out", "#0071E3"),
    ("spring", "Lock demo · 0.5 s spring / 0.2 bounce", "#F3455B")
]):
    for row, (metric, label) in enumerate([("progress", "Progress"), ("velocity", "Velocity / second")]):
        ax = axes[row, col]
        ax.plot(data["time_ms"], data[f"{prefix}_{metric}"], color=color, linewidth=2.5)
        ax.grid(alpha=.15)
        ax.set_ylabel(label)
        ax.set_xlabel("Time · ms")
        ax.axhline(1 if row == 0 else 0, color="#9A9AA0", linestyle="--", linewidth=.8)
        if row == 0: ax.set_title(title, loc="left", fontsize=13, pad=12)
        ax.set_xlim(0, 800)
fig.supxlabel("Native SwiftUI UnitCurve + Spring samples from the shipped tokens. Buttons and stick tracking respond immediately.", fontsize=10)
fig.savefig(root / "docs/design/onboarding-motion.png", dpi=160)
fig.savefig(root / "docs/design/onboarding-motion.svg")
peak = max(data["spring_progress"])
print(f"Spring peak: {peak:.5f}; overshoot: {(peak - 1) * 100:.2f}%; page ends at 240 ms")

import SwiftUI

/// Shared with the motion-curve exporter so the graphs describe the shipped timing.
enum OnboardingMotion {
    static let pageDuration = 0.24
    static let pageCurve = UnitCurve.bezier(startControlPoint: UnitPoint(x: 0.23, y: 1), endControlPoint: UnitPoint(x: 0.32, y: 1))
    static let demonstrationSpring = Spring(duration: 0.5, bounce: 0.2)
    static let page = Animation.timingCurve(0.23, 1, 0.32, 1, duration: pageDuration)
    static let demonstration = Animation.spring(duration: demonstrationSpring.duration, bounce: demonstrationSpring.bounce)
}

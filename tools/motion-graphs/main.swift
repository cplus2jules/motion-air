import Foundation
import SwiftUI

// Compile with OnboardingMotion.swift to sample the same native curves used by the tour.
let spring = OnboardingMotion.demonstrationSpring
print("time_ms,page_progress,page_velocity,spring_progress,spring_velocity")
for index in 0...240 {
    let time = Double(index) / 300
    let progress = min(1, time / OnboardingMotion.pageDuration)
    let page = OnboardingMotion.pageCurve.value(at: progress)
    let pageVelocity = progress < 1 ? OnboardingMotion.pageCurve.velocity(at: progress) / OnboardingMotion.pageDuration : 0
    let value: Double = spring.value(target: 1, time: time)
    let velocity: Double = spring.velocity(target: 1, time: time)
    print("\(time * 1000),\(page),\(pageVelocity),\(value),\(velocity)")
}

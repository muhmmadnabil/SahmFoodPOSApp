import SwiftUI

@main
struct SahmFoodPOSiOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.keyboard)
        }
    }
}

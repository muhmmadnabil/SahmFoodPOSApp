import SwiftUI

@main
struct RestaurantPOSiOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.keyboard)
        }
    }
}

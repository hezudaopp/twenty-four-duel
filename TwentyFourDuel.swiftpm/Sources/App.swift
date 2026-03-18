import SwiftUI

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        .portrait
    }
}

@main
struct TwentyFourDuelApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            GameView()
                .preferredColorScheme(.dark)
                .persistentSystemOverlays(.hidden)
                .statusBarHidden()
                .onAppear {
                    SoundManager.shared.startBGM()
                }
        }
    }
}

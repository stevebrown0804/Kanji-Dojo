import SwiftUI
import ComposeApp
import WanaKana
import AVFoundation

@main
struct KanjiDojoApp: App {
    
    let kotlinApplication: IosKotlinApplication
    
    init() {
        kotlinApplication = IosKotlinApplication(
            logger: SwiftNativeLogger(),
            japaneseUtils: SwiftWanakanaJapaneseUtils(),
            kanaTtsManagerProvider: { voiceData in SwiftTtsKanaManager(voiceData) },
            backupArchiveHandlerProvider: { SwiftBackupArchiveHandler() }
        )
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()

        }
    }
    
}

class SwiftNativeLogger : CoreNativeLogger {
    
    func logInfo(message: String) {
        NSLog(message)
    }
    
    func logError(message: String) {
        NSLog(message)

    }
    
}

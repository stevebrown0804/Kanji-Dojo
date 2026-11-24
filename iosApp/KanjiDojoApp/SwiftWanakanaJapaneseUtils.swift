import ComposeApp
import WanaKana

class SwiftWanakanaJapaneseUtils: CoreJapaneseUtils {
    func isHiragana(_ receiver: unichar) -> Bool {
        WanaKana.isHiragana(charToString(receiver))
    }
    
    func isKana(_ receiver: unichar) -> Bool {
        WanaKana.isKana(charToString(receiver))
    }
    
    func isKanji(_ receiver: unichar) -> Bool {
        WanaKana.isKanji(charToString(receiver))
    }
    
    func isKatakana(_ receiver: unichar) -> Bool {
        WanaKana.isKatakana(charToString(receiver))
    }
    
    func kanaToRomaji(_ receiver: String) -> String {
        return WanaKana.toRomaji(receiver)!
    }

    func readingInputToHiragana(_ receiver: String) -> String {
        let normalized = receiver.precomposedStringWithCompatibilityMapping
        return normalized.applyingTransform(.latinToHiragana, reverse: false) ?? normalized
    }
    
    func charToString(_ value: unichar) -> String {
        String.init(format: "%C", value)
    }
    
}

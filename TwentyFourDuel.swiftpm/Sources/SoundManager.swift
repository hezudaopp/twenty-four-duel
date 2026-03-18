import AVFoundation

@MainActor
final class SoundManager {
    static let shared = SoundManager()

    private var players: [String: AVAudioPlayer] = [:]
    private var bgmPlayer: AVAudioPlayer?

    private init() {
        try? AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)
        preload(["correct", "wrong", "tap", "timeout", "tick", "gameover"])
        prepareBGM()
    }

    private func preload(_ names: [String]) {
        for name in names {
            if let url = Bundle.main.url(forResource: name, withExtension: "wav") {
                players[name] = try? AVAudioPlayer(contentsOf: url)
                players[name]?.prepareToPlay()
            }
        }
    }

    private func prepareBGM() {
        guard let url = Bundle.main.url(forResource: "bgm", withExtension: "wav") else { return }
        bgmPlayer = try? AVAudioPlayer(contentsOf: url)
        bgmPlayer?.numberOfLoops = -1
        bgmPlayer?.volume = 0.35
        bgmPlayer?.prepareToPlay()
    }

    func play(_ name: String) {
        guard let player = players[name] else { return }
        player.currentTime = 0
        player.play()
    }

    func startBGM() {
        bgmPlayer?.play()
    }

    func stopBGM() {
        bgmPlayer?.stop()
        bgmPlayer?.currentTime = 0
    }
}

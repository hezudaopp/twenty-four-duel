// swift-tools-version: 5.8
import PackageDescription
import AppleProductTypes

let package = Package(
    name: "TwentyFourDuel",
    platforms: [
        .iOS("16.0")
    ],
    products: [
        .iOSApplication(
            name: "TwentyFourDuel",
            targets: ["AppModule"],
            bundleIdentifier: "com.example.twentyfourduel",
            teamIdentifier: "RYTWF48TVQ",
            displayVersion: "1.0",
            bundleVersion: "1",
            appIcon: .asset("AppIcon"),
            accentColor: .presetColor(.cyan),
            supportedDeviceFamilies: [
                .pad,
                .phone
            ],
            supportedInterfaceOrientations: [
                .portrait
            ],
            appCategory: .games,
            additionalInfoPlistContentFilePath: "Sources/AdditionalInfo.plist"
        )
    ],
    targets: [
        .executableTarget(
            name: "AppModule",
            path: "Sources",
            resources: [
                .process("Resources")
            ]
        )
    ]
)

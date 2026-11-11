import ExpoModulesCore

/// iOS has no public API for apps to toggle silent mode or a Focus, so this only keeps the
/// schedule so the shared UI works; silencing has to be set up through Settings > Focus.
public class SilentModeModule: Module {
  private static let windowsKey = "quietHours.windows"
  private static let quietModeKey = "quietHours.quietMode"
  private static let importantPeopleKey = "quietHours.importantPeople"
  private static let repeatCallersEnabledKey = "quietHours.repeatCallersEnabled"
  private static let repeatCallersThresholdKey = "quietHours.repeatCallersThreshold"
  private static let defaultRepeatCallersThreshold = 3

  public func definition() -> ModuleDefinition {
    Name("SilentMode")

    Function("refresh") {
      return self.status()
    }

    Function("silenceFor") { (_: Int) in
      return self.status()
    }

    Function("cancelManualSilence") {
      return self.status()
    }

    Function("setQuietMode") { (quietMode: String) in
      UserDefaults.standard.set(quietMode, forKey: Self.quietModeKey)
      return self.status()
    }

    Function("setWindows") { (serializedWindows: String) in
      UserDefaults.standard.set(serializedWindows, forKey: Self.windowsKey)
      return self.status()
    }

    Function("setImportantPeople") { (serializedPeople: String) in
      UserDefaults.standard.set(serializedPeople, forKey: Self.importantPeopleKey)
      return self.status()
    }

    Function("setRepeatCallers") { (enabled: Bool, threshold: Int) in
      UserDefaults.standard.set(enabled, forKey: Self.repeatCallersEnabledKey)
      UserDefaults.standard.set(threshold, forKey: Self.repeatCallersThresholdKey)
      return self.status()
    }

    Function("openNotificationAccessSettings") {
      self.openAppSettings()
    }

    Function("openAppDetailsSettings") {
      self.openAppSettings()
    }

    Function("requestIgnoreBatteryOptimisation") {
      self.openAppSettings()
    }

    Function("openDndAccessSettings") {
      self.openAppSettings()
    }

    Function("openExactAlarmSettings") {
      self.openAppSettings()
    }
  }

  private func status() -> [String: Any?] {
    return [
      "supported": false,
      "quietMode": UserDefaults.standard.string(forKey: Self.quietModeKey) ?? "silent",
      "hasDndAccess": false,
      "canScheduleExactAlarms": false,
      "isSilentNow": false,
      "manualSilenceUntil": nil,
      "nextChangeAt": nil,
      "windowsJson": UserDefaults.standard.string(forKey: Self.windowsKey) ?? "[]",
      "importantPeopleJson": UserDefaults.standard.string(forKey: Self.importantPeopleKey) ?? "[]",
      "repeatCallers": [
        "enabled": UserDefaults.standard.bool(forKey: Self.repeatCallersEnabledKey),
        "threshold": UserDefaults.standard.object(forKey: Self.repeatCallersThresholdKey) as? Int
          ?? Self.defaultRepeatCallersThreshold
      ],
      "setup": [
        "hasNotificationAccess": false,
        "hasPhoneAccess": false,
        "hasContactsAccess": false,
        "isBatteryOptimisationIgnored": false,
        "lastWhatsAppSeenAt": nil
      ] as [String: Any?]
    ]
  }

  private func openAppSettings() {
    DispatchQueue.main.async {
      if let settingsUrl = URL(string: UIApplication.openSettingsURLString) {
        UIApplication.shared.open(settingsUrl)
      }
    }
  }
}

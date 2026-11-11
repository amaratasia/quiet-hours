import ExpoModulesCore

/// iOS has no public API for apps to toggle silent mode or a Focus, so this only keeps the
/// schedule so the shared UI works; silencing has to be set up through Settings > Focus.
public class SilentModeModule: Module {
  private static let windowsKey = "quietHours.windows"

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

    Function("setWindows") { (serializedWindows: String) in
      UserDefaults.standard.set(serializedWindows, forKey: Self.windowsKey)
      return self.status()
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
      "hasDndAccess": false,
      "canScheduleExactAlarms": false,
      "isSilentNow": false,
      "manualSilenceUntil": nil,
      "nextChangeAt": nil,
      "windowsJson": UserDefaults.standard.string(forKey: Self.windowsKey) ?? "[]"
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

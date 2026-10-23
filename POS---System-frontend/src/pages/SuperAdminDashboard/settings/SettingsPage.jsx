import React from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { User, Shield } from "lucide-react";
import ProfileSettingsForm from "./components/ProfileSettingsForm";
import SecuritySettingsForm from "./components/SecuritySettingsForm";
import { useSettingsState } from "./components/useSettingsState";

const SettingsTabTrigger = ({ value, children }) => (
  <TabsTrigger value={value} className="flex items-center gap-2">
    {children}
  </TabsTrigger>
);

export default function SettingsPage() {
  const {
    profileData,
    profileLoading,
    profileSaving,
    passwordSaving,
    passwordData,
    showPasswords,
    handleProfileUpdate,
    handlePasswordChange,
    handleProfileFieldChange,
    handlePasswordFieldChange,
    handleShowPasswordToggle,
  } = useSettingsState();

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Settings</h2>
        <p className="text-muted-foreground">Manage your administrator profile and password.</p>
      </div>

      <Tabs defaultValue="profile" className="space-y-6">
        <TabsList className="grid w-full grid-cols-2">
          <SettingsTabTrigger value="profile">
            <User className="w-4 h-4" />
            Profile
          </SettingsTabTrigger>
          <SettingsTabTrigger value="security">
            <Shield className="w-4 h-4" />
            Security
          </SettingsTabTrigger>
        </TabsList>

        <TabsContent value="profile">
          <ProfileSettingsForm
            profileData={profileData}
            onUpdate={handleProfileUpdate}
            onFieldChange={handleProfileFieldChange}
            loading={profileLoading}
            saving={profileSaving}
          />
        </TabsContent>

        <TabsContent value="security">
          <SecuritySettingsForm
            passwordData={passwordData}
            showPasswords={showPasswords}
            onPasswordChange={handlePasswordFieldChange}
            onShowPasswordToggle={handleShowPasswordToggle}
            onUpdate={handlePasswordChange}
            loading={passwordSaving}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}

import { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useToast } from "@/components/ui/use-toast";
import { getUserProfile } from "@/Redux Toolkit/features/user/userThunks";
import api from "@/utils/api";

export const useSettingsState = () => {
  const dispatch = useDispatch();
  const { userProfile, loading: profileLoading } = useSelector((state) => state.user);
  const { toast } = useToast();
  const [profileSaving, setProfileSaving] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);

  const [profileData, setProfileData] = useState({
    fullName: "",
    email: "",
    phone: "",
  });

  useEffect(() => {
    const token = localStorage.getItem("jwt");
    if (token) {
      dispatch(getUserProfile(token));
    }
  }, [dispatch]);

  useEffect(() => {
    if (userProfile) {
      setProfileData({
        fullName: userProfile.fullName || "",
        email: userProfile.email || "",
        phone: userProfile.phone || userProfile.mobile || "",
      });
    }
  }, [userProfile]);

  const [passwordData, setPasswordData] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });

  const [showPasswords, setShowPasswords] = useState({
    current: false,
    new: false,
    confirm: false,
  });

  const handleProfileUpdate = async () => {
    const token = localStorage.getItem("jwt");
    if (!token) {
      toast({ title: "Sign in required", description: "Please sign in again to update your profile.", variant: "destructive" });
      return;
    }

    setProfileSaving(true);
    try {
      const response = await api.put("/api/users/profile", {
        fullName: profileData.fullName,
        phone: profileData.phone,
      }, { headers: { Authorization: `Bearer ${token}` } });
      setProfileData((current) => ({
        ...current,
        fullName: response.data.fullName || current.fullName,
        phone: response.data.phone || "",
      }));
      dispatch(getUserProfile(token));
      toast({ title: "Profile updated", description: "Your name and phone number have been saved." });
    } catch (error) {
      toast({
        title: "Profile update failed",
        description: error.response?.data?.message || "Could not save your profile. Please try again.",
        variant: "destructive",
      });
    } finally {
      setProfileSaving(false);
    }
  };

  const handlePasswordChange = async () => {
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      toast({
        title: "Password Mismatch",
        description: "New password and confirm password do not match.",
        variant: "destructive",
      });
      return;
    }

    if (passwordData.newPassword.length < 8) {
      toast({
        title: "Weak Password",
        description: "Password must be at least 8 characters long.",
        variant: "destructive",
      });
      return;
    }

    const token = localStorage.getItem("jwt");
    if (!token) {
      toast({ title: "Sign in required", description: "Please sign in again to change your password.", variant: "destructive" });
      return;
    }

    setPasswordSaving(true);
    try {
      await api.put("/api/users/password", {
        currentPassword: passwordData.currentPassword,
        newPassword: passwordData.newPassword,
      }, { headers: { Authorization: `Bearer ${token}` } });
      toast({ title: "Password changed", description: "Your new password has been saved." });
      setPasswordData({ currentPassword: "", newPassword: "", confirmPassword: "" });
    } catch (error) {
      toast({
        title: "Password change failed",
        description: error.response?.data?.message || "Could not change your password. Check your current password and try again.",
        variant: "destructive",
      });
    } finally {
      setPasswordSaving(false);
    }
  };

  const handleProfileFieldChange = (field, value) => {
    setProfileData((prev) => ({ ...prev, [field]: value }));
  };

  const handlePasswordFieldChange = (field, value) => {
    setPasswordData((prev) => ({ ...prev, [field]: value }));
  };
  
  const handleShowPasswordToggle = (field) => {
    setShowPasswords((prev) => ({ ...prev, [field]: !prev[field] }));
  };
  
  return {
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
    handleShowPasswordToggle
  };
};

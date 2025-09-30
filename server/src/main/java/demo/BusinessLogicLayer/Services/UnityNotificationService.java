package demo.BusinessLogicLayer.Services;

public interface UnityNotificationService {
    void sendLockerStatus(String lockerId, boolean isLocked);
}
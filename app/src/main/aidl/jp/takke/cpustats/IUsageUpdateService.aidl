package jp.takke.cpustats;

import jp.takke.cpustats.IUsageUpdateCallback;

oneway interface IUsageUpdateService {

    void registerCallback(IUsageUpdateCallback callback);
    
    void unregisterCallback(IUsageUpdateCallback callback);
    
    void stopResident();

    void startResident();
    
    void reloadSettings();
}
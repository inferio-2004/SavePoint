package com.example.Savepoint.Steam;

import com.example.Savepoint.Auth.AuthProvider;
import com.example.Savepoint.Auth.UserAuth;
import com.example.Savepoint.Auth.UserAuthJpaRepositry;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

@Service
@RequiredArgsConstructor
public class SteamSyncService {

    private final UserAuthJpaRepositry userAuthJpaRepositry;
    private final SteamLibraryImportHelper steamLibraryImportHelper;

    @Scheduled(cron = "0 0 0 * * *") // every 24 hours
    public void syncAllSteamLibraries() {
        List<UserAuth> steamUsers = userAuthJpaRepositry.findByProvider(AuthProvider.STEAM);
        for (UserAuth auth : steamUsers) {
            try {
                steamLibraryImportHelper.importSteamLibrary(auth.getUser().getId(), auth.getProviderUserId());
            } catch (RejectedExecutionException e) {
                // Log the error and continue with the next user
                System.err.println("thread queue is full");
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

}
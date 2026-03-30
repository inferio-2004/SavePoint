package com.example.Savepoint.User;

import com.example.Savepoint.Auth.AuthProvider;
import com.example.Savepoint.Auth.UserAuth;
import com.example.Savepoint.Auth.UserAuthJpaRepositry;
import com.example.Savepoint.Search.ElasticSearchIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserProfileJpaRepositry userProfileJpaRepositry;
    private final UserAuthJpaRepositry userAuthJpaRepositry;
    private final ElasticSearchIndexService elasticsearchIndexService;


    public Optional<UserProfileDTO> findBySteamId(String steamId) {
        return userAuthJpaRepositry
                .findByProviderAndProviderUserId(AuthProvider.STEAM, steamId)
                .map(userAuth -> {
                    UserProfile profile = userAuth.getUser();
                    return new UserProfileDTO(profile.getId(), profile.getDisplayName(), profile.getAvatarUrl());
                });
    }

    public Optional<UserProfileDTO> findByEmail(String email) {
        return userAuthJpaRepositry
                .findByProviderAndMailId(AuthProvider.MANUAL, email)
                .map(userAuth -> {
                    UserProfile profile = userAuth.getUser();
                    return new UserProfileDTO(profile.getId(), profile.getDisplayName(), profile.getAvatarUrl());
                });
    }

    @Transactional
    public UserProfileDTO createSteamUser(String steamId, String displayName, String avatarUrl) {
        UserProfile profile = UserProfile.builder()
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .build();
        UserProfile savedProfile = userProfileJpaRepositry.save(profile);

        UserAuth auth = UserAuth.builder()
                .provider(AuthProvider.STEAM)
                .providerUserId(steamId)
                .user(savedProfile)
                .build();
        userAuthJpaRepositry.save(auth);
        elasticsearchIndexService.indexUser(savedProfile);
        return new UserProfileDTO(savedProfile.getId(), savedProfile.getDisplayName(), savedProfile.getAvatarUrl());
    }



    @Transactional
    public UserProfileDTO createManualUser(UserRegisterDTO user) {
        // For simplicity, we are not hashing the password here. In production, always hash passwords!
        UserProfile profile = UserProfile.builder()
                .displayName(user.username())
                .avatarUrl(null)
                .build();
        UserProfile savedProfile = userProfileJpaRepositry.save(profile);

        UserAuth auth = UserAuth.builder()
                .provider(AuthProvider.MANUAL)
                .passwordHash(user.password())
                .mailId(user.mail())
                .user(savedProfile)
                .build();
        userAuthJpaRepositry.save(auth);
        elasticsearchIndexService.indexUser(savedProfile);
        return new UserProfileDTO(savedProfile.getId(), savedProfile.getDisplayName(), savedProfile.getAvatarUrl());
    }
}
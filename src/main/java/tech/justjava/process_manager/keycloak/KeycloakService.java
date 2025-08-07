package tech.justjava.process_manager.keycloak;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.justjava.process_manager.chat.entity.Conversation;
import tech.justjava.process_manager.chat.entity.User;
import tech.justjava.process_manager.chat.repository.ConversationRepository;
import tech.justjava.process_manager.chat.repository.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KeycloakService {


    private final KeycloakFeignClient keycloakClient;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;

    @Value("${keycloak.client-id}")
    String clientId;

    @Value("${keycloak.client-secret}")
    String clientSecret;

    private static final String grantType = "client_credentials";
    private String token;
    private Instant tokenExpiry;

    private void setToken(Map<String, Object> token){
        this.token = "Bearer "+ token.get("access_token");
        this.tokenExpiry = Instant.now().plusSeconds((Integer) token.get("expires_in"));
    }
    private String getToken(){
        if (this.token != null && this.tokenExpiry != null){
            if(this.tokenExpiry.isAfter(Instant.now()) ){
                return this.token;
            }
        }
        return null;
    }

    public String getAccessToken(){
        if (getToken() != null){
            return getToken();
        }
        Map<String,String> parmMaps= new HashMap<>();
        parmMaps.put("client_id",clientId);
        parmMaps.put("client_secret",clientSecret);
        parmMaps.put("grant_type",grantType);
        Map<String, Object> token = keycloakClient.getAccessToken(parmMaps);
        setToken(token);
//        System.out.println("Access token: " + token);
        return getToken();
    }

    public List<UserDTO> getRealmUsers(){
        List<Map<String, Object>> users;
        users = keycloakClient.getUsers(getAccessToken());
        List<UserDTO> userDTOs = new ArrayList<>();
        for (Map<String, Object> user : users) {
                UserDTO userDTO = UserDTO.builder()
                    .userId((String) user.get("id"))
                    .firstName((String) user.get("firstName"))
                    .lastName((String) user.get("lastName"))
                    .email((String) user.get("email"))
                    .status((Boolean) user.get("enabled")?"Enabled":"Disabled")
                    .group(null)
                    .build();
            userDTOs.add(userDTO);
        }
        return userDTOs;
    }

    public List<UserDTO> getUsers(){
        List<UserGroup> userGroup = userGroupRepository.findAll();
        List<UserDTO> userDTOs = new ArrayList<>();
        for (UserGroup group : userGroup) {
            userDTOs.addAll(getAllUserInGroup(group));
        }
        return userDTOs;
    }

    public List<UserDTO> getAllUserInGroup(UserGroup group) {
        List<UserDTO> userDTOs = new ArrayList<>();
        List<Map<String, Object>> users = keycloakClient.getAllUserInGroup(getAccessToken(), group.getGroupId());
        for (Map<String, Object> user : users) {
            UserDTO userDTO = UserDTO.builder()
                    .userId((String) user.get("id"))
                    .firstName((String) user.get("firstName"))
                    .lastName((String) user.get("lastName"))
                    .email((String) user.get("email"))
                    .status((Boolean) user.get("enabled")?"Enabled":"Disabled")
                    .group(group.getGroupName())
                    .build();
            userDTOs.add(userDTO);
        }
        return userDTOs;
    }

    private User mapClientUserToUser(UserGroup group, Map<String, Object> clientUser) {
        if (userRepository.existsByUserId((String)clientUser.get("id"))){
            User user = userRepository.findByUserId((String) clientUser.get("id"));
            return mapUser(group, clientUser, user);
        }
        User user = new User();
        return mapUser(group, clientUser, user);
    }

    private User mapUser(UserGroup group, Map<String, Object> clientUser, User user) {
        user.setUserId((String) clientUser.get("id"));
        user.setFirstName((String) clientUser.get("firstName"));
        user.setLastName((String) clientUser.get("lastName"));
        user.setEmail((String) clientUser.get("email"));
        user.setStatus((Boolean) clientUser.get("enabled"));
        user.setUserGroup(group);
        return user;
    }

    public void createUserInGroup(Map<String, String> params){
        Map<String, Object> user = new HashMap<>();
        user.put("username", params.get("username"));
        user.put("firstName", params.get("firstName"));
        user.put("lastName", params.get("lastName"));
        user.put("email", params.get("email"));
        user.put("enabled", params.get("status"));

        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", "1234");
        credential.put("temporary", true);
        user.put("credentials", List.of(credential));

        try {
            ResponseEntity<Void> response = keycloakClient.createUser(getAccessToken(), user);
            if (response.getStatusCode() != HttpStatus.CREATED) {
                System.out.println("Failed to create user: " + response.getStatusCode());
                return;
            }
        } catch (FeignException e) {
            if (e.status() != HttpStatus.CONFLICT.value()) {
                System.out.println("\nFailed to create user::: " + e.getMessage());
                return;
            }
        }
        Map<String, Object> user1 = getUserByEmail(params.get("email"));
        if (user1 == null) {
            System.out.println("User not found after creation.");
            return;
        }

        UserGroup group = userGroupRepository.findByGroupNameIgnoreCase(params.get("groups"));
        userRepository.save(mapUser(group, user1, new User()));
        String userId = (String) user1.get("userId");
        addUserToGroup(userId ,params.get("groups"));
    }

    public Map<String, Object> getUserByEmail(String email){

        ResponseEntity<List<Map<String, Object>>> response = keycloakClient.getUserByEmail(getAccessToken(),email);
        if (response.getBody() == null || response.getBody().isEmpty()) {
            System.out.println("User not found after creation.");
            return null;
        }
        return response.getBody().get(0);
    }

    public void addUserToGroup(String userId, String groupName){

        UserGroup group = userGroupRepository.findByGroupNameIgnoreCase(groupName);
        String groupId = group.getGroupId();
        Map<String, Object> groupRef = new HashMap<>();
        groupRef.put("id", groupId);

        ResponseEntity<Void> response = keycloakClient.addUserToGroup(getAccessToken(), userId, groupId, groupRef);
        group.setMembers(group.getMembers() + 1);
        userGroupRepository.save(group);
        System.out.println("Added user to group: " + response.getStatusCode());
    }
    public void createGroup(String groupName, String description) {
        var group = UserGroup.builder().groupName(groupName).description(description).build();
        Map<String, Object> body = new HashMap<>();
        body.put("name", groupName);
        body.put("attributes", Map.of("description", List.of(description)));

        keycloakClient.createGroup(getAccessToken(), body);
        userGroupRepository.save(group);
        syncGroups();
    }

    public void updateGroup(Map<String,String> params) {
        String groupId = params.get("id");
        UserGroup userGroup = userGroupRepository.findByGroupId(groupId);
        userGroup.setGroupName(params.get("name"));
        userGroup.setDescription(params.get("description"));

        Map<String, Object> body = new HashMap<>();
        body.put("id", userGroup.getGroupId());
        body.put("name", params.get("name"));
        body.put("attributes", Map.of("description", List.of(params.get("description"))));
        keycloakClient.updateGroup(getAccessToken(), userGroup.getGroupId(), body);
        userGroupRepository.save(userGroup);
    }

    public void updateUser(String userId, Map<String, String> params) {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", params.get("firstName"));
        body.put("lastName", params.get("lastName"));
        body.put("email", params.get("email"));
        body.put("username", params.get("username"));
        body.put("enabled", params.get("status"));

        keycloakClient.updateUser(getAccessToken(), userId, body);

        User user = userRepository.findByUserId(userId);
        UserGroup group = userGroupRepository.findByGroupNameIgnoreCase(params.get("groups"));
        UserGroup oldGroup = user.getUserGroup();
        oldGroup.setMembers(oldGroup.getMembers() - 1);
        userGroupRepository.save(oldGroup);
        user.setFirstName(params.get("firstName"));
        user.setLastName(params.get("lastName"));
        user.setEmail(params.get("email"));
        user.setStatus(Boolean.valueOf(params.get("status")));
        user.setUserGroup(group);
        userRepository.save(user);

        addUserToGroup(userId ,params.get("groups"));
        System.out.println("Successfully updated user: " + userId);
    }

    @Transactional
    public void deleteUser(String userId) {
        keycloakClient.deleteUser(getAccessToken(), userId);
        User user = userRepository.findByUserId(userId);
        UserGroup group = user.getUserGroup();
        group.setMembers(group.getMembers() - 1);
        userGroupRepository.save(group);
        deleteUser(user);
        System.out.println("Successfully deleted user: " + userId);
    }

    @Transactional
    public void deleteGroup(String groupId) {
        ResponseEntity<Void> response = keycloakClient.deleteGroup(getAccessToken(), groupId);
        if (response.getStatusCode().is2xxSuccessful()) {
            UserGroup group = userGroupRepository.findByGroupId(groupId);
            for (User user : group.getUsers()) {
                user.setUserGroup(null);
            }
            userRepository.saveAll(group.getUsers());
            userGroupRepository.delete(group);
            System.out.println("Group deleted successfully.");
        } else {
            System.out.println("Failed to delete group: " + response.getStatusCode());
        }
    }
    @Transactional
    protected void deleteUser(User user) {
        for (Conversation conversation : user.getConversations()) {
            conversation.getMembers().remove(user);
        }
        conversationRepository.saveAll(user.getConversations());
        user.getConversations().clear();
        userRepository.delete(user);
    }

    public void syncGroups() {
        System.out.println("\nSyncing Groups:::");
        List<Map<String, Object>> realmGroups = keycloakClient.getRealmGroups(getAccessToken());
        Map<String, UserGroup> existingGroupsMap = userGroupRepository.findAll().stream()
                .collect(Collectors.toMap(g -> g.getGroupName().toLowerCase(), g -> g));

        List<UserGroup> groupsToSave = new ArrayList<>();
        Set<String> realmGroupNames = new HashSet<>();
        for (Map<String, Object> realmGroup : realmGroups) {
            String groupName = (String) realmGroup.get("name");
            String lowerCaseGroupName = groupName.toLowerCase();
            String groupId = (String) realmGroup.get("id");
            String groupDescription = (String) realmGroup.get("description");

            realmGroupNames.add(lowerCaseGroupName);
            UserGroup userGroup = existingGroupsMap.get(lowerCaseGroupName);
            if (userGroup!=null) {
                userGroup.setGroupId(groupId);
                userGroup.setGroupName(groupName);
                userGroup.setDescription(groupDescription);
                groupsToSave.add(userGroup);
            } else {
                groupsToSave.add(UserGroup.builder()
                        .groupName(groupName)
                        .groupId(groupId)
                        .description(groupDescription)
                        .build());
            }
        }
        if (!groupsToSave.isEmpty()) {
            userGroupRepository.saveAll(groupsToSave);
        }
        List<UserGroup> groupsToDelete = existingGroupsMap.values().stream()
                .filter(group -> !realmGroupNames.contains(group.getGroupName().toLowerCase()))
                .toList();
        if (!groupsToDelete.isEmpty()) {
            userGroupRepository.deleteAll(groupsToDelete);
        }
        System.out.println("\nDone Syncing Groups");
    }

    @Transactional
    protected void syncClientUsers(){
        System.out.println("\nSyncing Users:::");
        List<UserGroup> userGroup = userGroupRepository.findAll();
        Map<String, User> savedUsersMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        List<Map<String, Object>> clientUsers = keycloakClient.getUsers(getAccessToken());
        Map<String, User> usersToSave = new HashMap<>();

        for (Map<String, Object> clientUser : clientUsers) {
            String userId = (String) clientUser.get("id");
            User user = savedUsersMap.getOrDefault(userId, new User());
            User mapped = mapUser(user.getUserGroup(), clientUser, user);
            usersToSave.put(userId, mapped);
        }
        for (UserGroup group : userGroup) {
            List<Map<String, Object>> clientUsersInGroup = keycloakClient.getAllUserInGroup(getAccessToken(), group.getGroupId());
            group.setMembers(clientUsersInGroup.size());
            for (Map<String, Object> user : clientUsersInGroup) {
                String userId = (String) user.get("id");
                User existing = usersToSave.get(userId);
                if (existing != null) {
                    mapUser(group, user, existing);
                }
            }
        }
        userGroupRepository.saveAll(userGroup);
        if (!usersToSave.isEmpty()) {
            userRepository.saveAll(usersToSave.values());
        }
        Set<String> userIds = usersToSave.keySet();
        List<User> usersToDelete = savedUsersMap.values().stream()
                .filter(user -> !userIds.contains(user.getUserId()))
                .toList();

        if (!usersToDelete.isEmpty()) {
            usersToDelete.forEach(this::deleteUser);
        }
        System.out.println("\nDone Syncing Users");
    }

    @Transactional()
    public void syncKeycloak(){
        syncGroups();
        syncClientUsers();
    }
}

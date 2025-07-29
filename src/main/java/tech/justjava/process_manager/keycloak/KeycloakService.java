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
import java.util.Optional;
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

   /* public List<UserDTO> getUsers() {
        List<UserGroup> localGroups = userGroupRepository.findAll();
        Map<String, String> groupIdToName = localGroups.stream()
                .filter(g -> g.getGroupId() != null)
                .collect(Collectors.toMap(UserGroup::getGroupId, UserGroup::getGroupName));

        List<UserDTO> userDTOs = new ArrayList<>();

        List<Map<String, Object>> keycloakUsers = keycloakClient.getUsers(getAccessToken());

        for (Map<String, Object> user : keycloakUsers) {
            String userId = (String) user.get("id");

            List<Map<String, Object>> userGroups = keycloakClient.getUserGroups(getAccessToken(), userId);

            for (Map<String, Object> group : userGroups) {
                String groupId = (String) group.get("id");
                String groupName = groupIdToName.get(groupId);

                if (groupName != null) {
                    UserDTO userDTO = UserDTO.builder()
                            .firstName((String) user.get("firstName"))
                            .lastName((String) user.get("lastName"))
                            .email((String) user.get("email"))
                            .status("Active")
                            .group(groupName)
                            .build();
                    userDTOs.add(userDTO);
                }
            }
        }

        return userDTOs;
    }*/

    public List<UserDTO> getUsers(){
        List<UserGroup> userGroup = userGroupRepository.findAll();
        List<UserDTO> userDTOs = new ArrayList<>();
        for (UserGroup group : userGroup) {
            userDTOs.addAll(getAllUserInGroup(group));
        }
        return userDTOs;
    }
    public UserDTO getSingleUser(String userId) {
        List<UserGroup> userGroups = userGroupRepository.findAll();

        for (UserGroup group : userGroups) {
            List<UserDTO> usersInGroup = getAllUserInGroup(group);
            for (UserDTO user : usersInGroup) {
                if (user.getUserId().equals(userId)) {
                    return user;
                }
            }
        }

        return null;
    }

    public List<UserGroup> getUserGroups(){
        return userGroupRepository.findAll();
    }
    public UserGroup getSingleGroup(String groupId){
        return userGroupRepository.findByGroupId(groupId);
    }

    public void syncGroups() {
        System.out.println("\nSyncing Groups:::");
        List<Map<String, Object>> realmGroups = keycloakClient.getRealmGroups(getAccessToken());
        List<UserGroup> existingGroups = userGroupRepository.findAll();
        List<UserGroup> groupsToSave = new ArrayList<>();

        Set<String> realmGroupNames = new HashSet<>();
        for (Map<String, Object> realmGroup : realmGroups) {
            String groupName = (String) realmGroup.get("name");
            String groupId = (String) realmGroup.get("id");
            String groupDescription = (String) realmGroup.get("description");

            realmGroupNames.add(groupName.toLowerCase());

            Optional<UserGroup> matched = existingGroups.stream()
                    .filter(g -> g.getGroupName().equalsIgnoreCase(groupName))
                    .findFirst();
            if (matched.isPresent()) {
                UserGroup userGroup = matched.get();
                if (userGroup.getGroupId() == null) {
                    userGroup.setGroupId(groupId);
                    groupsToSave.add(userGroup);
                }
            } else {
                UserGroup newGroup = UserGroup.builder()
                        .groupName(groupName)
                        .groupId(groupId)
                        .description(groupDescription)
                        .build();
                groupsToSave.add(newGroup);
            }
        }
        if (!groupsToSave.isEmpty()) {
            userGroupRepository.saveAll(groupsToSave);
        }
        List<UserGroup> groupsToDelete = existingGroups.stream()
                .filter(group -> !realmGroupNames.contains(group.getGroupName().toLowerCase()))
                .collect(Collectors.toList());
        if (!groupsToDelete.isEmpty()) {
            userGroupRepository.deleteAll(groupsToDelete);
        }
        System.out.println("\nDone Syncing Groups");
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
        String userId = getUserId(params.get("email"));
        if (userId == null) {
            System.out.println("User not found after creation.");
            return;
        }
        addUserToGroup(userId ,params.get("groups"));
    }

    public String getUserId(String email){

        ResponseEntity<List<Map<String, Object>>> response = keycloakClient.getUserByEmail(getAccessToken(),email);
        if (response.getBody() == null || response.getBody().isEmpty()) {
            System.out.println("User not found after creation.");
            return null;
        }
        Map<String, Object> userInfo = response.getBody().get(0);
        String userId = (String) userInfo.get("id");
        return userId;
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
        body.put("enabled", true);

        keycloakClient.updateUser(getAccessToken(), userId, body);

        addUserToGroup(userId ,params.get("groups"));
        System.out.println("Successfully updated user: " + userId);
    }
    public void deleteUser(String userId) {
        keycloakClient.deleteUser(getAccessToken(), userId);
        System.out.println("Successfully deleted user: " + userId);
        //TODO Reduce user count from UserGroup
    }

    @Transactional
    public void deleteGroup(String groupId) {
        ResponseEntity<Void> response = keycloakClient.deleteGroup(getAccessToken(), groupId);
        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.println("Group deleted successfully.");
            userGroupRepository.deleteByGroupId(groupId);
        } else {
            System.out.println("Failed to delete group: " + response.getStatusCode());
        }
    }
    @Transactional
    protected void deleteUser(User user) {

        // Remove from conversations
        for (Conversation conversation : user.getConversations()) {
            conversation.getMembers().remove(user);
        }
        conversationRepository.saveAll(user.getConversations());
        user.getConversations().clear();

        // Delete user
        userRepository.delete(user);
    }

    @Transactional
    protected void syncClientUsers(){
        System.out.println("\nSyncing Users:::");
        List<UserGroup> userGroup = userGroupRepository.findAll();
        List<User> savedUsers = userRepository.findAll();
        List<Map<String, Object>> clientUsers = keycloakClient.getUsers(getAccessToken());
        List<User> usersToSave = new ArrayList<>();
        
        Set<String> userIds = new HashSet<>();

        for (Map<String, Object> clientUser : clientUsers) {
            String userId = (String) clientUser.get("id");
            Optional<User> matched = savedUsers.stream()
                    .filter(u -> u.getUserId().equals(userId))
                    .findFirst();
            userIds.add(userId);
            if (matched.isPresent()){
                User user = matched.get();
                mapUser(user.getUserGroup(),clientUser, user);
                usersToSave.add(user);
            }else {
                User user = new User();
                User mappedUser = mapUser(null, clientUser, user);
                usersToSave.add(mappedUser);
            }
        }
        for (UserGroup group : userGroup) {
            List<Map<String, Object>> clientUsersInGroup = keycloakClient.getAllUserInGroup(getAccessToken(), group.getGroupId());
            for (Map<String, Object> user : clientUsersInGroup) {
                String userId = (String) user.get("id");
                Optional<User> optionalUser = usersToSave.stream()
                        .filter(u -> userId.equals(u.getUserId())).findFirst();
                optionalUser.ifPresent(value -> mapUser(group, user, value));
            }
        }
        if (!usersToSave.isEmpty()) {
            userRepository.saveAll(usersToSave);
        }
        List<User> usersToDelete = savedUsers.stream()
                .filter(user -> !userIds.contains(user.getUserId()))
                .toList();

        if (!usersToDelete.isEmpty()) {
            usersToDelete.forEach(this::deleteUser);
        }
        System.out.println("\nDone Syncing Users");

    }

    @Transactional
    public void syncKeycloak(){
        syncGroups();
        syncClientUsers();
    }
}

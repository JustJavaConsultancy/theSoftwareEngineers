package tech.justjava.process_manager.keycloak;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name="KeycloakFeignClient",url="${keycloak.base-url}")
public interface KeycloakFeignClient {

    @PostMapping(path = "/realms/attorneyAI/protocol/openid-connect/token",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    Map<String, Object> getAccessToken(Map<String,?> paramMap);

    @GetMapping("/admin/realms/attorneyAI/users")
    List<Map<String, Object>> getUsers(@RequestHeader(value = "Authorization") String authorizationHeader);

    @GetMapping("/admin/realms/attorneyAI/users/{userId}")
    Map<String, Object> getUser(@RequestHeader(value = "Authorization") String authorizationHeader,
                                       @PathVariable String userId);

    @GetMapping("/admin/realms/attorneyAI/groups")
    List<Map<String, Object>> getRealmGroups(
            @RequestHeader(value = "Authorization") String authorizationHeader);

    @GetMapping("/admin/realms/attorneyAI/groups/{groupId}/members")
    List<Map<String, Object>> getAllUserInGroup(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable("groupId") String groupId
    );

    @GetMapping("/admin/realms/attorneyAI/users")
    ResponseEntity<List<Map<String, Object>>> getUserByEmail(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @RequestParam(name = "email") String email
    );

    @PostMapping("/admin/realms/attorneyAI/users")
    ResponseEntity<Void> createUser(@RequestHeader(value = "Authorization")
                                    String authorizationHeader, Map<String,Object> user);

    @PutMapping("/admin/realms/attorneyAI/users/{userId}/groups/{groupId}")
    ResponseEntity<Void> addUserToGroup(@RequestHeader(value = "Authorization") String authorizationHeader,
                             @PathVariable String userId,
                             @PathVariable String groupId,
                             @RequestBody Map<String, Object> groupBody
    );

    @GetMapping("/admin/realms/attorneyAI/users/{id}/groups")
    List<Map<String, Object>> getUserGroups(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable("id") String userId
    );

    @PutMapping("/admin/realms/attorneyAI/groups/{groupId}")
    ResponseEntity<Void> updateGroup(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable String groupId,
            @RequestBody Map<String, Object> groupBody);

    @PutMapping("/admin/realms/attorneyAI/users/{userId}")
    ResponseEntity<Void> updateUser(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable String userId,
            @RequestBody Map<String, Object> body);

    @PostMapping("/admin/realms/attorneyAI/groups")
    void createGroup(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            Map<String,?> paramMap
            );

    @DeleteMapping("/admin/realms/attorneyAI/users/{userId}")
    void deleteUser(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable String userId
    );

    @DeleteMapping("/admin/realms/attorneyAI/groups/{groupId}")
    ResponseEntity<Void> deleteGroup(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable String groupId
    );

}

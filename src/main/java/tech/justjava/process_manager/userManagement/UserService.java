package tech.justjava.process_manager.userManagement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.justjava.process_manager.chat.entity.User;
import tech.justjava.process_manager.chat.repository.UserRepository;
import tech.justjava.process_manager.keycloak.UserDTO;
import tech.justjava.process_manager.keycloak.UserGroup;
import tech.justjava.process_manager.keycloak.UserGroupRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;

    public List<UserDTO> getUsers(){
        List<User> users = userRepository.findAll();
        List<UserDTO> userDTOs = new ArrayList<>();
        for (User user : users) {
            userDTOs.add(mapUserTOUserDTO(user));
        }
        return userDTOs;
    }
    public List<UserGroup> getUserGroups(){
        return userGroupRepository.findAll();
    }
    public UserDTO getSingleUser(String userId) {
        User client = userRepository.findByUserId(userId);
        return mapUserTOUserDTO(client);
    }
    public UserGroup getSingleGroup(String groupId){
        return userGroupRepository.findByGroupId(groupId);
    }

    private UserDTO mapUserTOUserDTO(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .status(user.getStatus())
                .group(user.getUserGroup()!=null? user.getUserGroup().getGroupName():"")
                .avatar(user.getAvatar())
                .build();
    }
}

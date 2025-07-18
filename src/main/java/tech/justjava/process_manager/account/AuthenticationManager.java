package tech.justjava.process_manager.account;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthenticationManager {
    public Object get(String fieldName){
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        DefaultOidcUser defaultOidcUser = (DefaultOidcUser) authentication.getPrincipal();
//        System.out.println(" The token here =="+defaultOidcUser.getClaims());
        return defaultOidcUser.getClaims().get(fieldName);
    }

    public Boolean isMerchant(){
        List<String> groups = (List<String>) get("group");
        if(groups==null)
            return true;
        return false;
    }

    public Boolean isComplianceOfficer(){

        List<String> groups = (List<String>) get("group");
        if(groups==null)
            return false;

        return groups
                .stream()
                .anyMatch(group->"/compliance".equalsIgnoreCase(group));
    }

    public Boolean isCustomerSupport(){
        List<String> groups = (List<String>) get("group");
        if(groups==null)
            return false;
        return groups
                .stream()
                .anyMatch(group->"/customer-support".equalsIgnoreCase(group));
    }

    public Boolean isPgAdmin(){
        List<String> groups = (List<String>) get("group");
        if(groups==null)
            return false;
        return groups
                .stream()
                .anyMatch(group->"/pgAdmin".equalsIgnoreCase(group));
    }

}

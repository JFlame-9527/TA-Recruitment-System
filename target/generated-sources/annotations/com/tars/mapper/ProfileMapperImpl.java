package com.tars.mapper;

import com.tars.entity.bean.TAProfile;
import com.tars.entity.dto.mo.ProfileDTO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-10T00:26:49+0800",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.45.0.v20260224-0835, environment: Java 21.0.10 (Eclipse Adoptium)"
)
public class ProfileMapperImpl implements ProfileMapper {

    @Override
    public ProfileDTO toMOProfileDTO(TAProfile profile, String feedback) {
        if ( profile == null && feedback == null ) {
            return null;
        }

        ProfileDTO profileDTO = new ProfileDTO();

        if ( profile != null ) {
            profileDTO.setAge( profile.getAge() );
            profileDTO.setCollege( profile.getCollege() );
            profileDTO.setEmail( profile.getEmail() );
            profileDTO.setGender( profile.getGender() );
            profileDTO.setGrade( profile.getGrade() );
            profileDTO.setMajor( profile.getMajor() );
            profileDTO.setName( profile.getName() );
            profileDTO.setPhone( profile.getPhone() );
            profileDTO.setResumeName( profile.getResumeName() );
            profileDTO.setResumePath( profile.getResumePath() );
            List<String> list = profile.getSkills();
            if ( list != null ) {
                profileDTO.setSkills( new ArrayList<String>( list ) );
            }
            profileDTO.setUserId( profile.getUserId() );
        }
        profileDTO.setFeedback( feedback );

        return profileDTO;
    }
}

package com.tars.mapper;

import com.tars.entity.bean.TAProfile;
import com.tars.entity.dto.ta.ProfileDTO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-10T00:01:13+0800",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.45.0.v20260224-0835, environment: Java 21.0.10 (Eclipse Adoptium)"
)
public class ProfileMapperImpl implements ProfileMapper {

    @Override
    public ProfileDTO toTAProfileDTO(TAProfile taProfile) {
        if ( taProfile == null ) {
            return null;
        }

        ProfileDTO profileDTO = new ProfileDTO();

        profileDTO.setAge( taProfile.getAge() );
        profileDTO.setCollege( taProfile.getCollege() );
        profileDTO.setEmail( taProfile.getEmail() );
        profileDTO.setGender( taProfile.getGender() );
        profileDTO.setGrade( taProfile.getGrade() );
        profileDTO.setId( taProfile.getId() );
        profileDTO.setMajor( taProfile.getMajor() );
        profileDTO.setName( taProfile.getName() );
        profileDTO.setPhone( taProfile.getPhone() );
        profileDTO.setResumeName( taProfile.getResumeName() );
        profileDTO.setResumePath( taProfile.getResumePath() );
        List<String> list = taProfile.getSkills();
        if ( list != null ) {
            profileDTO.setSkills( new ArrayList<String>( list ) );
        }

        return profileDTO;
    }
}

package com.tars.mapper;

import com.tars.entity.bean.Position;
import com.tars.entity.dto.mo.PosBriefDTO;
import com.tars.entity.dto.mo.PosDetailDTO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-10T00:26:50+0800",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.45.0.v20260224-0835, environment: Java 21.0.10 (Eclipse Adoptium)"
)
public class PosMapperImpl implements PosMapper {

    @Override
    public PosBriefDTO toMOPosBriefDTO(Position position) {
        if ( position == null ) {
            return null;
        }

        PosBriefDTO posBriefDTO = new PosBriefDTO();

        posBriefDTO.setPosId( position.getId() );
        posBriefDTO.setDeadline( position.getDeadline() );
        posBriefDTO.setModuleCode( position.getModuleCode() );
        posBriefDTO.setModuleName( position.getModuleName() );
        posBriefDTO.setPostDate( position.getPostDate() );
        posBriefDTO.setStatus( position.getStatus() );
        posBriefDTO.setTitle( position.getTitle() );

        posBriefDTO.setVacancyNum( position.getRequiredNum() - position.getOfferedNum() );
        posBriefDTO.setPendingNum( position.getAppliedNum() - position.getOfferedNum() - position.getRejectedNum() );

        return posBriefDTO;
    }

    @Override
    public PosDetailDTO toMOPosDetailDTO(Position position) {
        if ( position == null ) {
            return null;
        }

        PosDetailDTO posDetailDTO = new PosDetailDTO();

        posDetailDTO.setPosId( position.getId() );
        posDetailDTO.setAppliedNum( position.getAppliedNum() );
        posDetailDTO.setDeadline( position.getDeadline() );
        posDetailDTO.setDescription( position.getDescription() );
        posDetailDTO.setDuration( position.getDuration() );
        posDetailDTO.setEndDate( position.getEndDate() );
        posDetailDTO.setModuleCode( position.getModuleCode() );
        posDetailDTO.setModuleName( position.getModuleName() );
        posDetailDTO.setOfferedNum( position.getOfferedNum() );
        posDetailDTO.setPostDate( position.getPostDate() );
        posDetailDTO.setRejectedNum( position.getRejectedNum() );
        posDetailDTO.setRequiredNum( position.getRequiredNum() );
        List<String> list = position.getSkills();
        if ( list != null ) {
            posDetailDTO.setSkills( new ArrayList<String>( list ) );
        }
        posDetailDTO.setStartDate( position.getStartDate() );
        posDetailDTO.setStatus( position.getStatus() );
        posDetailDTO.setTitle( position.getTitle() );
        posDetailDTO.setWeeklyWorkload( position.getWeeklyWorkload() );

        return posDetailDTO;
    }
}

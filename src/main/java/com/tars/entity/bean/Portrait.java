package com.tars.entity.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class Portrait {
    private String id;

    private List<Float> skills;

    private List<Float> experience;

    private List<Float> softSkills;

    public Portrait(List<Float> skillsVector, List<Float> experienceVector, List<Float> softSkillsVector) {
        this.id = UUID.randomUUID().toString();
        this.skills = skillsVector;
        this.experience= experienceVector;
        this.softSkills = softSkillsVector;
    }
}

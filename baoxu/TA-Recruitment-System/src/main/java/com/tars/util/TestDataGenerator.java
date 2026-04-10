package com.tars.util;

import com.tars.entity.bean.*;
import com.tars.repository.JsonRepository;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Test Data Generator for TA-Recruitment-System
 * Generates realistic test data for all entities
 *
 * Usage: Run main() method to populate data/ directory with test data
 *
 * @author mei1234567554
 * @version 1.0.0
 * @since 2026/4/7
 */
public class TestDataGenerator {

    private static final JsonRepository<User> userRepo = new JsonRepository<>(User.class);
    private static final JsonRepository<TAProfile> taProfileRepo = new JsonRepository<>(TAProfile.class);
    private static final JsonRepository<MOProfile> moProfileRepo = new JsonRepository<>(MOProfile.class);
    private static final JsonRepository<Position> positionRepo = new JsonRepository<>(Position.class);
    private static final JsonRepository<Application> applicationRepo = new JsonRepository<>(Application.class);

    public static void main(String[] args) {
        System.out.println("=== Starting Test Data Generation ===\n");

        try {
            List<User> users = generateUsers();
            List<TAProfile> taProfiles = generateTAProfiles(users);
            List<MOProfile> moProfiles = generateMOProfiles(users);
            List<Position> positions = generatePositions(users);
            List<Application> applications = generateApplications(positions, users);

            saveAllData(users, taProfiles, moProfiles, positions, applications);

            System.out.println("\n=== Test Data Generation Complete ===");
            System.out.println("Total Users: " + users.size());
            System.out.println("Total TA Profiles: " + taProfiles.size());
            System.out.println("Total MO Profiles: " + moProfiles.size());
            System.out.println("Total Positions: " + positions.size());
            System.out.println("Total Applications: " + applications.size());
            System.out.println("\nData saved to data/ directory successfully!");

        } catch (Exception e) {
            System.err.println("Error generating test data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<User> generateUsers() {
        System.out.println("Generating users...");
        List<User> users = new ArrayList<>();

        String adminId = UUID.randomUUID().toString();
        User admin = new User();
        admin.setId(adminId);
        admin.setName("admin");
        admin.setPassword(hashPassword("admin123"));
        admin.setRole(0);
        admin.setStatus(0);
        admin.setCreateAt(Timestamp.valueOf(LocalDateTime.now().minusDays(90)));
        admin.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));
        admin.setLastLoginAt(Timestamp.valueOf(LocalDateTime.now().minusHours(2)));
        users.add(admin);

        String[] taNames = {"zhangsan", "lisi", "wangwu", "zhaoliu", "sunqi"};
        for (int i = 0; i < taNames.length; i++) {
            User user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setName(taNames[i]);
            user.setPassword(hashPassword("password123"));
            user.setRole(1);
            user.setStatus(0);
            user.setCreateAt(Timestamp.valueOf(LocalDateTime.now().minusDays(60 - i * 5)));
            user.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));
            user.setLastLoginAt(Timestamp.valueOf(LocalDateTime.now().minusHours(i + 1)));
            users.add(user);
        }

        String[] moNames = {"mo_chen", "mo_li", "mo_wang"};
        for (int i = 0; i < moNames.length; i++) {
            User user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setName(moNames[i]);
            user.setPassword(hashPassword("password123"));
            user.setRole(2);
            user.setStatus(0);
            user.setCreateAt(Timestamp.valueOf(LocalDateTime.now().minusDays(50 - i * 10)));
            user.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));
            user.setLastLoginAt(Timestamp.valueOf(LocalDateTime.now().minusHours(i + 3)));
            users.add(user);
        }

        System.out.println("   ✓ Generated " + users.size() + " users (1 admin, " + (taNames.length) + " TAs, " + moNames.length + " MOs)");
        return users;
    }

    private static List<TAProfile> generateTAProfiles(List<User> users) {
        System.out.println("Generating TA profiles...");
        List<TAProfile> profiles = new ArrayList<>();

        List<User> taUsers = users.stream().filter(u -> u.getRole() == 1).toList();

        String[] colleges = {"School of Computer Science", "School of Engineering", "School of Business", "School of Mathematics", "School of Physics"};
        String[] majors = {"Computer Science", "Software Engineering", "Information Systems", "Data Science", "Cybersecurity"};
        String[] grades = {"Freshman", "Sophomore", "Junior", "Senior", "Graduate"};
        String[][] skillSets = {
                {"Java", "Python", "Spring Boot", "MySQL"},
                {"JavaScript", "React", "Node.js", "MongoDB"},
                {"C++", "Algorithms", "Data Structures", "Linux"},
                {"Machine Learning", "TensorFlow", "Python", "Statistics"},
                {"Web Development", "HTML/CSS", "Vue.js", "PostgreSQL"}
        };

        for (int i = 0; i < taUsers.size(); i++) {
            User user = taUsers.get(i);
            TAProfile profile = new TAProfile();
            profile.setId(UUID.randomUUID().toString());
            profile.setUserId(user.getId());
            profile.setName(user.getName().substring(0, 1).toUpperCase() + user.getName().substring(1));
            profile.setGender(i % 2 == 0 ? "Male" : "Female");
            profile.setAge(String.valueOf(20 + i));
            profile.setCollege(colleges[i % colleges.length]);
            profile.setMajor(majors[i % majors.length]);
            profile.setGrade(grades[i % grades.length]);
            profile.setSkills(Arrays.asList(skillSets[i % skillSets.length]));
            profile.setEmail(user.getName() + "@university.edu");
            profile.setPhone("+86-138-" + String.format("%04d", 1000 + i) + "-" + String.format("%04d", 2000 + i));
            profile.setResumeName("resume_" + user.getName() + ".pdf");
            profile.setResumePath("resumes/resume_" + user.getName() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf");
            profile.setCreateAt(user.getCreateAt());
            profile.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));
            profiles.add(profile);
        }

        System.out.println("   ✓ Generated " + profiles.size() + " TA profiles");
        return profiles;
    }

    private static List<MOProfile> generateMOProfiles(List<User> users) {
        System.out.println("Generating MO profiles...");
        List<MOProfile> profiles = new ArrayList<>();

        List<User> moUsers = users.stream().filter(u -> u.getRole() == 2).toList();

        String[] colleges = {"School of Computer Science", "School of Engineering", "School of Business"};

        for (int i = 0; i < moUsers.size(); i++) {
            User user = moUsers.get(i);
            MOProfile profile = new MOProfile();
            profile.setId(UUID.randomUUID().toString());
            profile.setUserId(user.getId());
            profile.setName(user.getName().substring(0, 1).toUpperCase() + user.getName().substring(1).replace("_", " "));
            profile.setCollege(colleges[i % colleges.length]);
            profile.setEmail(user.getName() + "@company.com");
            profile.setPhone("+86-139-" + String.format("%04d", 3000 + i) + "-" + String.format("%04d", 4000 + i));
            profiles.add(profile);
        }

        System.out.println("   ✓ Generated " + profiles.size() + " MO profiles");
        return profiles;
    }

    private static List<Position> generatePositions(List<User> users) {
        System.out.println("Generating positions...");
        List<Position> positions = new ArrayList<>();

        List<User> moUsers = users.stream().filter(u -> u.getRole() == 2).toList();

        String[] titles = {
                "Teaching Assistant - Introduction to Programming",
                "Teaching Assistant - Data Structures and Algorithms",
                "Teaching Assistant - Web Development",
                "Teaching Assistant - Database Systems",
                "Teaching Assistant - Machine Learning",
                "Teaching Assistant - Software Engineering",
                "Teaching Assistant - Computer Networks",
                "Teaching Assistant - Operating Systems",
                "Lab Assistant - Computer Graphics",
                "Grader - Discrete Mathematics"
        };

        String[] descriptions = {
                "Assist with teaching introductory programming courses in Java and Python. Help students with assignments and conduct lab sessions.",
                "Support the Data Structures course by grading assignments, holding office hours, and leading review sessions.",
                "Help teach modern web development including HTML, CSS, JavaScript, and React framework.",
                "Assist with database course covering SQL, normalization, and database design principles.",
                "Support machine learning course by helping students with projects and practical implementations.",
                "Assist in software engineering course focusing on agile methodologies and team projects.",
                "Help teach computer networking fundamentals including TCP/IP, routing, and network security.",
                "Support operating systems course covering process management, memory management, and file systems.",
                "Assist with computer graphics lab sessions and help students with OpenGL projects.",
                "Grade assignments and exams for discrete mathematics course."
        };

        String[] moduleCodes = {"CS101", "CS201", "CS301", "CS302", "CS401", "SE301", "CS303", "CS304", "CS402", "MA201"};
        String[] moduleNames = {
                "Introduction to Programming", "Data Structures", "Web Development",
                "Database Systems", "Machine Learning", "Software Engineering",
                "Computer Networks", "Operating Systems", "Computer Graphics", "Discrete Mathematics"
        };

        String[][] skills = {
                {"Java", "Python", "Programming Fundamentals"},
                {"Data Structures", "Algorithms", "Problem Solving"},
                {"HTML", "CSS", "JavaScript", "React"},
                {"SQL", "Database Design", "Normalization"},
                {"Python", "Machine Learning", "Statistics"},
                {"Agile", "Git", "Software Design"},
                {"Networking", "TCP/IP", "Security"},
                {"Operating Systems", "C/C++", "System Programming"},
                {"OpenGL", "Computer Graphics", "Mathematics"},
                {"Mathematics", "Logic", "Proof Writing"}
        };

        int positionIndex = 0;
        for (int moIdx = 0; moIdx < moUsers.size(); moIdx++) {
            User moUser = moUsers.get(moIdx);
            int positionsPerMO = (positionIndex + 3 <= titles.length) ? 3 : titles.length - positionIndex;

            for (int j = 0; j < positionsPerMO && positionIndex < titles.length; j++, positionIndex++) {
                Position position = new Position();
                position.setId(UUID.randomUUID().toString());
                position.setTitle(titles[positionIndex]);
                position.setDescription(descriptions[positionIndex]);
                position.setModuleCode(moduleCodes[positionIndex]);
                position.setModuleName(moduleNames[positionIndex]);
                position.setSkills(Arrays.asList(skills[positionIndex]));
                position.setPostUserId(moUser.getId());
                position.setWeeklyWorkload(10 + (positionIndex % 5) * 2);
                position.setDuration(16);
                position.setRequiredNum(2 + (positionIndex % 3));
                position.setOfferedNum(0);
                position.setAppliedNum(0);
                position.setRejectedNum(0);

                LocalDateTime now = LocalDateTime.now();
                position.setStartDate(Timestamp.valueOf(now.plusDays(7)));
                position.setEndDate(Timestamp.valueOf(now.plusDays(120)));
                position.setPostDate(Timestamp.valueOf(now.minusDays(positionIndex * 3)));
                position.setDeadline(Timestamp.valueOf(now.plusDays(14)));
                position.setCreateAt(Timestamp.valueOf(now.minusDays(positionIndex * 3 + 5)));
                position.setUpdateAt(Timestamp.valueOf(now));
                position.setStatus(positionIndex < 7 ? 0 : (positionIndex < 9 ? 1 : 2));

                positions.add(position);
            }
        }

        System.out.println("   ✓ Generated " + positions.size() + " positions");
        return positions;
    }

    private static List<Application> generateApplications(List<Position> positions, List<User> users) {
        System.out.println("Generating applications...");
        List<Application> applications = new ArrayList<>();

        List<User> taUsers = users.stream().filter(u -> u.getRole() == 1).toList();

        int applicationId = 0;
        for (int posIdx = 0; posIdx < positions.size(); posIdx++) {
            Position position = positions.get(posIdx);
            if (position.getStatus() != 0) continue;

            int numApplications = 2 + (posIdx % 4);
            for (int appIdx = 0; appIdx < numApplications && appIdx < taUsers.size(); appIdx++) {
                User taUser = taUsers.get((applicationId) % taUsers.size());

                Application application = new Application();
                application.setId(UUID.randomUUID().toString());
                application.setPositionId(position.getId());
                application.setUserId(taUser.getId());

                int statusRoll = applicationId % 10;
                if (statusRoll < 5) {
                    application.setStatus(0);
                } else if (statusRoll < 7) {
                    application.setStatus(1);
                } else if (statusRoll < 9) {
                    application.setStatus(2);
                } else {
                    application.setStatus(3);
                }

                if (application.getStatus() == 1) {
                    application.setFeedback("Excellent qualifications and relevant experience. Recommended for offer.");
                } else if (application.getStatus() == 2) {
                    application.setFeedback("Thank you for your interest. Unfortunately, we have selected candidates with more relevant experience.");
                } else if (application.getStatus() == 3) {
                    application.setFeedback("");
                } else {
                    application.setFeedback("");
                }

                int daysAgo = 1 + (applicationId % 30);
                application.setApplyAt(Timestamp.valueOf(LocalDateTime.now().minusDays(daysAgo)));

                applications.add(application);
                applicationId++;
            }

            position.setAppliedNum((int) applications.stream()
                    .filter(a -> a.getPositionId().equals(position.getId()))
                    .count());
            position.setOfferedNum((int) applications.stream()
                    .filter(a -> a.getPositionId().equals(position.getId()) && a.getStatus() == 1)
                    .count());
            position.setRejectedNum((int) applications.stream()
                    .filter(a -> a.getPositionId().equals(position.getId()) && a.getStatus() == 2)
                    .count());
        }

        System.out.println("   ✓ Generated " + applications.size() + " applications");
        return applications;
    }

    private static void saveAllData(List<User> users, List<TAProfile> taProfiles,
                                    List<MOProfile> moProfiles, List<Position> positions,
                                    List<Application> applications) throws IOException {
        System.out.println("\nSaving data to JSON files...");

        userRepo.saveAllEntities(users);
        System.out.println("   ✓ Saved " + users.size() + " users to user.json");

        taProfileRepo.saveAllEntities(taProfiles);
        System.out.println("   ✓ Saved " + taProfiles.size() + " TA profiles to ta_profile.json");

        moProfileRepo.saveAllEntities(moProfiles);
        System.out.println("   ✓ Saved " + moProfiles.size() + " MO profiles to mo_profile.json");

        positionRepo.saveAllEntities(positions);
        System.out.println("   ✓ Saved " + positions.size() + " positions to position.json");

        applicationRepo.saveAllEntities(applications);
        System.out.println("   ✓ Saved " + applications.size() + " applications to application.json");
    }

    private static String hashPassword(String password) {
        return DigestUtils.md5Hex(password);
    }
}

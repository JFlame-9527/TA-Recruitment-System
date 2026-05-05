package com.tars.util;

import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.common.Message;
import com.tars.ai.PortraitGenerator;
import com.tars.config.ApplicationConfiguration;
import com.tars.config.QwenConfiguration;
import com.tars.entity.bean.*;
import com.tars.repository.JsonRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Test Data Generator for TA-Recruitment-System
 * Generates realistic test data for all entities with AI-enhanced descriptions
 *
 * @author mei1234567554
 * @version 3.0.0
 * @since 2026/4/7
 */
@Slf4j
public class TestDataGenerator {

    private final JsonRepository<User> userRepo = new JsonRepository<>(User.class);
    private final JsonRepository<TAProfile> taProfileRepo = new JsonRepository<>(TAProfile.class);
    private final JsonRepository<MOProfile> moProfileRepo = new JsonRepository<>(MOProfile.class);
    private final JsonRepository<Position> positionRepo = new JsonRepository<>(Position.class);
    private final JsonRepository<Application> applicationRepo = new JsonRepository<>(Application.class);

    @Getter
    private String resumeDir;

    private Generation generation;
    private final Random rd = new Random();

    private static volatile boolean alreadyGenerated = false;

    private TestDataGenerator() {
        this.resumeDir = ApplicationConfiguration.getInstance().getFileDir();
        log.info("TestDataGenerator initialized with resumeDir (from config): {}", this.resumeDir);
    }

    public static void run() {
        if (alreadyGenerated) {
            log.warn("Test data has already been generated. Skipping...");
            return;
        }

        TestDataGenerator generator = new TestDataGenerator();
        try {
            generator.generate();
            alreadyGenerated = true;
            log.info("Test data generation flag set to prevent re-generation");
        } catch (Exception e) {
            log.error("Test data generation failed", e);
            throw new RuntimeException("Failed to generate test data", e);
        } finally {
            generator.cleanup();
        }
    }

    private void generate() {
        log.info("=== Starting Test Data Generation ===");

        try {
            generation = new Generation();

            List<User> users = generateUsers();
            List<TAProfile> taProfiles = generateBasicTAProfiles(users);
            List<MOProfile> moProfiles = generateMOProfiles(users);
            List<Position> positions = generateBasicPositions(users);
            List<Application> applications = generateBasicApplications(positions, users);

            List<Portrait> portraits = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(2);

            ExecutorService executor = Executors.newFixedThreadPool(2);

            executor.submit(() -> {
                try {
                    generateCompleteTAProfiles(taProfiles, portraits);
                } finally {
                    latch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    generateCompletePositions(positions, portraits);
                } finally {
                    latch.countDown();
                }
            });

            latch.await();
            executor.shutdown();

            if (!executor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
                log.warn("Executor shutdown timeout, forced shutdown");
            }

            log.info("All portraits generated. Starting application feedback generation...");
            generateCompleteApplications(applications, positions, taProfiles);

            saveAllData(users, taProfiles, moProfiles, positions, applications, portraits);

            log.info("=== Test Data Generation Complete ===");
            log.info("Total Users: {}", users.size());
            log.info("Total TA Profiles: {}", taProfiles.size());
            log.info("Total MO Profiles: {}", moProfiles.size());
            log.info("Total Positions: {}", positions.size());
            log.info("Total Applications: {}", applications.size());
            log.info("Total Portraits: {}", portraits.size());
            log.info("Data saved to data/ directory successfully!");

        } catch (Exception e) {
            log.error("Error generating test data", e);
            throw new RuntimeException("Test data generation failed", e);
        }
    }

    private void cleanup() {
        generation = null;
        log.debug("TestDataGenerator resources cleaned up");
    }

    private List<User> generateUsers() throws IOException {
        log.info("Generating users...");
        List<User> users = userRepo.loadAllEntities();

        String[] taNames = {
                "zhangsan", "lisi", "wangwu", "zhaoliu", "sunqi",
                "zhouba", "wujiu", "zhengshi", "chenxiaoyu", "linfeng",
                "huangwei", "xujing", "zhuqiang", "suna", "majun"
        };
        for (int i = 0; i < taNames.length; i++) {
            User user = new User();
            user.setName(taNames[i]);
            user.setPassword(hashPassword("password123"));
            user.setRole(1);
            user.setStatus(0);
            user.setCreateAt(Timestamp.valueOf(LocalDateTime.now().minusDays(60 - i * 4)));
            user.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));
            user.setLastLoginAt(Timestamp.valueOf(LocalDateTime.now().minusHours((i % 10) + 1)));
            users.add(user);
        }

        String[] moNames = {
                "mo_chen", "mo_li", "mo_wang", "mo_zhang",
                "mo_liu", "mo_yang", "mo_zhao", "mo_huang"
        };
        for (int i = 0; i < moNames.length; i++) {
            User user = new User();
            user.setName(moNames[i]);
            user.setPassword(hashPassword("password123"));
            user.setRole(2);
            user.setStatus(0);
            user.setCreateAt(Timestamp.valueOf(LocalDateTime.now().minusDays(50 - i * 6)));
            user.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));
            user.setLastLoginAt(Timestamp.valueOf(LocalDateTime.now().minusHours((i % 8) + 2)));
            users.add(user);
        }

        log.info("Generated {} users ({} TAs, {} MOs)", users.size(), taNames.length, moNames.length);
        return users;
    }

    private List<TAProfile> generateBasicTAProfiles(List<User> users) {
        log.info("Generating TA profiles...");
        List<TAProfile> profiles = new ArrayList<>();

        List<User> taUsers = users.stream().filter(u -> u.getRole() == 1).toList();

        String[] colleges = {
                "School of Computer Science",
                "School of Engineering",
                "School of Business",
                "School of Mathematics",
                "School of Physics",
                "School of Chemistry",
                "School of Biology",
                "School of Economics",
                "School of Law",
                "School of Humanities"
        };

        String[] majors = {
                "Computer Science",
                "Software Engineering",
                "Information Systems",
                "Data Science",
                "Cybersecurity",
                "Mechanical Engineering",
                "Electrical Engineering",
                "Civil Engineering",
                "Business Administration",
                "Finance",
                "Applied Mathematics",
                "Statistics",
                "Molecular Biology",
                "Chemical Engineering",
                "International Law"
        };

        String[][] skillSets = {
                {"Java", "Python", "Spring Boot", "MySQL"},
                {"JavaScript", "React", "Node.js", "MongoDB"},
                {"C++", "Algorithms", "Data Structures", "Linux"},
                {"Machine Learning", "TensorFlow", "Python", "Statistics"},
                {"Web Development", "HTML/CSS", "Vue.js", "PostgreSQL"},
                {"CAD", "SolidWorks", "MATLAB", "Mechanical Design"},
                {"Circuit Design", "Verilog", "Embedded Systems", "IoT"},
                {"Structural Analysis", "AutoCAD", "Project Management", "Construction"},
                {"Financial Modeling", "Excel", "Risk Analysis", "Accounting"},
                {"Legal Research", "Contract Law", "Critical Thinking", "Writing"}
        };

        for (int i = 0; i < taUsers.size(); i++) {
            User user = taUsers.get(i);
            TAProfile profile = new TAProfile();
            profile.setUserId(user.getId());
            profile.setName(user.getName().substring(0, 1).toUpperCase() + user.getName().substring(1));
            profile.setGender(rd.nextInt(2) % 2 == 0 ? "Male" : "Female");
            profile.setAge(rd.nextInt(18, 30));
            profile.setCollege(colleges[rd.nextInt(colleges.length)]);
            profile.setMajor(majors[i % majors.length]);
            profile.setDegree(rd.nextInt(2) == 0 ? "Bachelor" : rd.nextInt(2) == 0 ? "Master" : "PhD");
            profile.setYear((i % 4) + 1);
            profile.setSkills(Arrays.asList(skillSets[i % skillSets.length]));
            profile.setEmail(user.getName() + "@bupt.edu.com");
            profile.setPhone("138" + String.format("%04d", 1000 + i) + String.format("%04d", 2000 + i));
            String resumeName = "resume_" + user.getName();
            profile.setResumeName(resumeName);

            // Store relative path with subdirectory (consistent with user uploads)
            profile.setResumePath("resumes" + File.separator + resumeName + ".md");

            profile.setCreateAt(user.getCreateAt());
            profile.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));
            profiles.add(profile);
        }

        log.info("Generated {} TA profiles", profiles.size());
        return profiles;
    }

    private List<MOProfile> generateMOProfiles(List<User> users) {
        log.info("Generating MO profiles...");
        List<MOProfile> profiles = new ArrayList<>();

        List<User> moUsers = users.stream().filter(u -> u.getRole() == 2).toList();

        String[] colleges = {"School of Computer Science", "School of Engineering", "School of Business"};

        for (int i = 0; i < moUsers.size(); i++) {
            User user = moUsers.get(i);
            MOProfile profile = new MOProfile();
            profile.setUserId(user.getId());
            profile.setName(user.getName().substring(0, 1).toUpperCase() + user.getName().substring(1).replace("_", " "));
            profile.setCollege(colleges[rd.nextInt(colleges.length)]);
            profile.setEmail(user.getName() + "@bupt.edu.com");
            profile.setPhone("139" + String.format("%04d", 3000 + i) + String.format("%04d", 4000 + i));
            profiles.add(profile);
        }

        log.info("Generated {} MO profiles", profiles.size());
        return profiles;
    }

    private List<Position> generateBasicPositions(List<User> users) {
        log.info("Generating positions...");
        List<Position> positions = new ArrayList<>();

        List<User> moUsers = users.stream().filter(u -> u.getRole() == 2 && u.getStatus() == 0).toList();

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
                "Grader - Discrete Mathematics",
                "Teaching Assistant - Mechanical Design",
                "Teaching Assistant - Circuit Analysis",
                "Teaching Assistant - Financial Accounting",
                "Teaching Assistant - Business Statistics",
                "Teaching Assistant - Organic Chemistry",
                "Lab Assistant - Molecular Biology",
                "Teaching Assistant - Calculus I",
                "Teaching Assistant - Linear Algebra",
                "Teaching Assistant - Contract Law",
                "Grader - English Literature"
        };

        String[] moduleCodes = {
                "CS101", "CS201", "CS301", "CS302", "CS401",
                "SE301", "CS303", "CS304", "CS402", "MA201",
                "ME201", "EE202", "BA301", "BA202", "CH301",
                "BI302", "MA101", "MA202", "LA401", "HU201"
        };

        String[] moduleNames = {
                "Introduction to Programming",
                "Data Structures and Algorithms",
                "Web Development",
                "Database Systems",
                "Machine Learning",
                "Software Engineering",
                "Computer Networks",
                "Operating Systems",
                "Computer Graphics",
                "Discrete Mathematics",
                "Mechanical Design Fundamentals",
                "Circuit Analysis and Design",
                "Financial Accounting Principles",
                "Business Statistics",
                "Organic Chemistry",
                "Molecular Biology Lab",
                "Calculus I",
                "Linear Algebra",
                "Contract Law",
                "English Literature"
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
                {"Mathematics", "Logic", "Proof Writing"},
                {"CAD", "SolidWorks", "Mechanical Drawing"},
                {"Circuit Design", "Multisim", "Electronics"},
                {"Accounting", "Excel", "Financial Reporting"},
                {"Statistics", "SPSS", "Data Analysis"},
                {"Chemistry", "Lab Safety", "Spectroscopy"},
                {"Biology", "PCR", "Microscopy"},
                {"Calculus", "Mathematical Analysis", "Problem Solving"},
                {"Linear Algebra", "Matrix Theory", "Abstract Algebra"},
                {"Legal Research", "Case Analysis", "Writing"},
                {"Literature", "Critical Reading", "Essay Writing"}
        };

        for (int i = 0; i < titles.length; i++) {
            Position position = new Position();
            position.setTitle(titles[i]);
            position.setModuleCode(moduleCodes[i]);
            position.setModuleName(moduleNames[i]);
            position.setSkills(Arrays.asList(skills[i]));
            position.setPostUserId(moUsers.get(rd.nextInt(moUsers.size())).getId());
            position.setWeeklyWorkload(rd.nextInt(1, 9) * 2);
            position.setDuration(rd.nextInt(1, 13));
            position.setRequiredNum(rd.nextInt(1, 7));
            position.setStartDate(Timestamp.valueOf(LocalDateTime.now().plusWeeks(rd.nextInt(2, 6))));
            position.setEndDate(Timestamp.valueOf(position.getStartDate().toLocalDateTime().plusWeeks(position.getDuration())));
            position.setDeadline(Timestamp.valueOf(position.getStartDate().toLocalDateTime().minusDays(rd.nextInt(5, 13))));
            position.setPostDate(Timestamp.valueOf(LocalDateTime.now().minusDays(rd.nextInt(60))));

            if (rd.nextBoolean()) {
                position.setMinGrade(rd.nextInt(1, 6) + (rd.nextBoolean() ? 0 : 10));
            }
            if (rd.nextBoolean()) {
                int offset = position.getMinGrade() > 10 ? 10 : 0;
                int maxGrade = Math.max(position.getMinGrade(), offset + rd.nextInt(1, 6));
                position.setMaxGrade(maxGrade);
            }

            position.setStatus(rd.nextInt(4));

            positions.add(position);
        }

        log.info("Generated {} positions", positions.size());
        return positions;
    }

    private List<Application> generateBasicApplications(List<Position> positions, List<User> users) {
        log.info("Generating applications...");
        List<Application> applications = new ArrayList<>();

        List<Position> openPositions = positions.stream()
                .filter(p -> rd.nextBoolean())
                .filter(p -> p.getStatus() == 0)
                .toList();

        List<User> taUsers = users.stream().filter(u -> u.getRole() == 1 && u.getStatus() == 0).toList();

        for (Position position : openPositions) {
            int numApplications = rd.nextInt(1, taUsers.size() / 3 * 2);
            for (int i = 0, userIdx = rd.nextInt(taUsers.size()); i < numApplications; i++, userIdx++) {
                if (userIdx >= taUsers.size()) {
                    userIdx = 0;
                }

                Application application = new Application();
                application.setPositionId(position.getId());
                application.setUserId(taUsers.get(userIdx).getId());
                application.setStatus(rd.nextInt(4));
                application.setApplyAt(Timestamp.valueOf(LocalDateTime.now().minusDays(rd.nextInt(30))));
                switch (application.getStatus()) {
                    case 0:
                        position.setAppliedNum(position.getAppliedNum() + 1);
                        break;
                    case 1:
                        position.setOfferedNum(position.getOfferedNum() + 1);
                        break;
                    case 2:
                        position.setRejectedNum(position.getRejectedNum() + 1);
                        break;
                }
                applications.add(application);
            }
        }

        log.info("Generated {} applications", applications.size());
        return applications;
    }

    private void generateCompleteTAProfiles(List<TAProfile> profiles, List<Portrait> portraits) {
        log.info("Starting AI generation for TA resumes and portraits...");

        PortraitGenerator portraitGenerator = new PortraitGenerator();

        // Get absolute path for file writing
        String absoluteResumeDir = com.tars.config.ApplicationConfiguration.getInstance().getFilePath();

        List<Portrait> generatedPortraits = profiles.parallelStream()
                .map(profile -> {
                    try {
                        String markdownResume = generateResumeMarkdown(
                                profile.getName(),
                                profile.getMajor(),
                                profile.getDegree(),
                                profile.getYear(),
                                profile.getCollege(),
                                profile.getSkills(),
                                profile.getEmail(),
                                profile.getPhone()
                        );

                        // Use absolute path to write file (includes subdirectory)
                        Path filePath = Paths.get(absoluteResumeDir, profile.getResumePath());

                        // Ensure parent directory exists
                        Path parentDir = filePath.getParent();
                        if (parentDir != null && !Files.exists(parentDir)) {
                            Files.createDirectories(parentDir);
                            log.debug("Created subdirectory: {}", parentDir);
                        }

                        Files.writeString(filePath, markdownResume, StandardCharsets.UTF_8);

                        File resumeFile = filePath.toFile();

                        if (!resumeFile.exists() || resumeFile.length() == 0) {
                            log.warn("Resume file not created or empty: {}", filePath);
                            return null;
                        }

                        Portrait portrait = portraitGenerator.generatePortrait(profile, resumeFile);
                        profile.setPortraitId(portrait.getId());

                        log.debug("Generated resume and portrait for: {}", profile.getResumeName());
                        return portrait;
                    } catch (Exception e) {
                        log.error("Failed to generate resume for {}", profile.getName(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        portraits.addAll(generatedPortraits);

        log.info("Completed TA generation: {}/{} successful", generatedPortraits.size(), profiles.size());
    }

    private void generateCompletePositions(List<Position> positions, List<Portrait> portraits) {
        log.info("Starting AI generation for position descriptions and portraits...");

        PortraitGenerator portraitGenerator = new PortraitGenerator();

        List<Portrait> generatedPortraits = positions.parallelStream()
                .map(position -> {
                    try {
                        String description = generatePositionDescription(
                                position.getTitle(),
                                position.getModuleName(),
                                position.getSkills()
                        );

                        position.setDescription(description);

                        Portrait portrait = portraitGenerator.generatePortrait(position);
                        position.setPortraitId(portrait.getId());

                        log.debug("Generated description and portrait for: {}", position.getTitle());
                        return portrait;
                    } catch (Exception e) {
                        log.error("Failed to generate description for position: {}", position.getTitle(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        portraits.addAll(generatedPortraits);

        log.info("Completed position generation: {}/{} successful", generatedPortraits.size(), positions.size());
    }

    private String generatePositionDescription(String title, String moduleName, List<String> skills) throws Exception {
        String apiKey = QwenConfiguration.getInstance().getApiKey();

        String prompt = """
                Generate a professional job description for a Teaching Assistant position.

                Position Title: %s
                Module Name: %s
                Required Skills: %s

                Requirements:
                - 60-100 words
                - Include key responsibilities
                - Mention required skills naturally
                - Professional academic tone
                - Start with action verbs

                Return ONLY the description text, no JSON formatting.
                """.formatted(title, moduleName, String.join(", ", skills));

        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(QwenConfiguration.getInstance().getQwen().getModel())
                .messages(Arrays.asList(
                        Message.builder().role("system").content("You are a professional academic job description writer.").build(),
                        Message.builder().role("user").content(prompt).build()
                ))
                .temperature(0.7f)
                .maxTokens(300)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        GenerationResult result = generation.call(param);
        return result.getOutput().getChoices().getFirst().getMessage().getContent().trim();
    }


    private void generateCompleteApplications(List<Application> applications, List<Position> positions, List<TAProfile> taProfiles) {
        log.info("Starting AI generation for application feedback...");

        List<Application> targetApplications = applications.stream()
                .filter(app -> app.getStatus() == 1 || app.getStatus() == 2)
                .toList();

        if (targetApplications.isEmpty()) {
            log.info("No applications with status 1 or 2 found. Skipping feedback generation.");
            return;
        }

        Map<String, Position> positionMap = positions.stream()
                .collect(Collectors.toMap(Position::getId, p -> p));

        Map<String, String> applicantNameMap = taProfiles.stream()
                .collect(Collectors.toMap(TAProfile::getUserId, TAProfile::getName));

        List<Application> successList = Collections.synchronizedList(new ArrayList<>());

        targetApplications.parallelStream()
                .forEach(application -> {
                    try {
                        Position position = positionMap.get(application.getPositionId());
                        if (position == null) {
                            log.warn("Position not found for application: {}", application.getId());
                            return;
                        }

                        String applicantName = applicantNameMap.getOrDefault(application.getUserId(), "Candidate");

                        String feedback = generateApplicationFeedback(application.getStatus(), position, applicantName);

                        application.setFeedback(feedback);

                        successList.add(application);

                        log.debug("Generated feedback for application: {} (Status: {}, Applicant: {})",
                                application.getId(),
                                application.getStatus() == 1 ? "Offered" : "Rejected",
                                applicantName);
                    } catch (Exception e) {
                        log.error("Failed to generate feedback for application: {}", application.getId(), e);
                    }
                });

        log.info("Completed application feedback generation: {}/{} successful",
                successList.size(), targetApplications.size());
    }

    private String generateApplicationFeedback(int status, Position position, String applicantName) throws Exception {
        String apiKey = QwenConfiguration.getInstance().getApiKey();

        String prompt = status == 1
                ? """
                Write a positive offer feedback for a TA application.

                Position: %s (%s)
                Applicant: %s

                Requirements:
                - 30-50 words
                - Highlight strengths and fit for the position
                - Professional and encouraging tone
                - Mention next steps briefly
                - Return ONLY the feedback text.
                """.formatted(position.getTitle(), position.getModuleCode(), applicantName)
                : """
                Write a rejection feedback for a TA application.

                Position: %s (%s)
                Applicant: %s

                Requirements:
                - 30-50 words
                - Constructive and polite
                - Mention areas for improvement or missing qualifications
                - Encourage future applications
                - Return ONLY the feedback text.
                """.formatted(position.getTitle(), position.getModuleCode(), applicantName);

        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(QwenConfiguration.getInstance().getQwen().getModel())
                .messages(Arrays.asList(
                        Message.builder().role("system").content("You are a professional HR manager providing application feedback.").build(),
                        Message.builder().role("user").content(prompt).build()
                ))
                .temperature(0.7f)
                .maxTokens(200)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        GenerationResult result = generation.call(param);
        return result.getOutput().getChoices().getFirst().getMessage().getContent().trim();
    }

    private String generateResumeMarkdown(String name, String major, String degree, int year,
                                          String college, List<String> skills,
                                          String email, String phone) throws Exception {
        String apiKey = QwenConfiguration.getInstance().getApiKey();

        String prompt = """
                Generate a professional resume in Markdown format for a university student applying for a Teaching Assistant position.

                Name: %s
                Major: %s
                Degree: %s (Year %d)
                College: %s
                Skills: %s
                Email: %s
                Phone: %s

                Requirements:
                - Use standard Markdown formatting
                - Include sections: Education, Technical Skills, Project Experience (2-3 projects), Achievements
                - Projects should be realistic for the major and year
                - 300-400 words total
                - Professional academic tone
                - Return ONLY the Markdown content, no explanations
                """.formatted(name, major, degree, year, college, String.join(", ", skills), email, phone);

        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(QwenConfiguration.getInstance().getQwenLong().getModel())
                .messages(Arrays.asList(
                        Message.builder().role("system").content("You are a professional resume writer for university students.").build(),
                        Message.builder().role("user").content(prompt).build()
                ))
                .temperature(0.7f)
                .maxTokens(800)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        GenerationResult result = generation.call(param);
        return result.getOutput().getChoices().getFirst().getMessage().getContent().trim();
    }

    private void saveAllData(List<User> users, List<TAProfile> taProfiles,
                             List<MOProfile> moProfiles, List<Position> positions,
                             List<Application> applications, List<Portrait> portraits) throws IOException {
        log.info("Saving data to JSON files...");

        userRepo.saveAllEntities(users);
        log.info("Saved {} users to user.json", users.size());

        taProfileRepo.saveAllEntities(taProfiles);
        log.info("Saved {} TA profiles to ta_profile.json", taProfiles.size());

        moProfileRepo.saveAllEntities(moProfiles);
        log.info("Saved {} MO profiles to mo_profile.json", moProfiles.size());

        positionRepo.saveAllEntities(positions);
        log.info("Saved {} positions to position.json", positions.size());

        applicationRepo.saveAllEntities(applications);
        log.info("Saved {} applications to application.json", applications.size());

        JsonRepository<Portrait> portraitRepo = new JsonRepository<>(Portrait.class);
        portraitRepo.saveAllEntities(portraits);
        log.info("Saved {} portraits to portrait.json", portraits.size());
    }

    private String hashPassword(String password) {
        return DigestUtils.md5Hex(password);
    }
}

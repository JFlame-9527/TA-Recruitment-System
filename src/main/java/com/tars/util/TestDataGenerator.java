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
 * Utility class for generating comprehensive test data for the TA-Recruitment-System.
 * <p>
 * This generator creates realistic test data for all system entities using a combination of:
 * <ul>
 *   <li><b>Template-based Generation</b>: Predefined arrays of names, majors, skills, etc.</li>
 *   <li><b>AI-enhanced Content</b>: Qwen AI generates unique resumes, position descriptions, and application feedback</li>
 *   <li><b>Vector Portrait Generation</b>: Creates embedding vectors for TA profiles and positions using {@link PortraitGenerator}</li>
 * </ul>
 * </p>
 * <p>
 * <b>Generated Entities:</b>
 * <ul>
 *   <li>{@link User}: 15 TA users (role=1) + 8 MO users (role=2) = 23 total</li>
 *   <li>{@link TAProfile}: Complete profiles with AI-generated Markdown resumes</li>
 *   <li>{@link MOProfile}: Basic profiles for all MO users</li>
 *   <li>{@link Position}: 20 teaching positions across various departments</li>
 *   <li>{@link Application}: Random applications with varied statuses (applied/offered/rejected/withdrawn)</li>
 *   <li>{@link Portrait}: Vector embeddings for all TA profiles and positions</li>
 * </ul>
 * </p>
 * <p>
 * <b>AI Integration:</b> Uses Alibaba DashScope Qwen API to generate:
 * <ol>
 *   <li><b>TA Resumes</b>: 300-400 word Markdown resumes with Education, Skills, Projects, Achievements sections</li>
 *   <li><b>Position Descriptions</b>: 60-100 word professional job descriptions</li>
 *   <li><b>Application Feedback</b>: 30-50 word offer/rejection messages personalized to each applicant</li>
 * </ol>
 * All AI generation runs in parallel using thread pools for performance.
 * </p>
 * <p>
 * <b>Execution Flow:</b>
 * <ol>
 *   <li>Generate basic user accounts (TAs and MOs) with hashed passwords</li>
 *   <li>Create TA/MO profiles with template data</li>
 *   <li>Generate 20 positions with random attributes</li>
 *   <li>Create random applications linking TAs to positions</li>
 *   <li><b>Parallel Phase 1</b>: Generate TA resumes (AI) + portraits (vector) concurrently</li>
 *   <li><b>Parallel Phase 2</b>: Generate position descriptions (AI) + portraits (vector) concurrently</li>
 *   <li>Generate application feedback (AI) for offered/rejected applications</li>
 *   <li>Save all entities to JSON files in data/ directory</li>
 * </ol>
 * </p>
 * <p>
 * <b>Safety Features:</b>
 * <ul>
 *   <li><b>Single Execution Guard</b>: Static flag prevents duplicate data generation</li>
 *   <li><b>Resource Cleanup</b>: Ensures Generation client is nullified after use</li>
 *   <li><b>Thread Safety</b>: Uses synchronized collections and CountDownLatch for parallel operations</li>
 *   <li><b>Error Handling</b>: Individual AI failures don't abort entire generation (logs and continues)</li>
 *   <li><b>Directory Creation</b>: Automatically creates subdirectories for resume files</li>
 * </ul>
 * </p>
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 * // Called during application startup or test initialization
 * TestDataGenerator.run();
 * 
 * // Data saved to:
 * // - data/user.json
 * // - data/ta_profile.json
 * // - data/mo_profile.json
 * // - data/position.json
 * // - data/application.json
 * // - data/portrait.json
 * // - resumes/resume_*.md (individual resume files)
 * }</pre>
 * </p>
 * <p>
 * <b>Configuration:</b> Requires valid Qwen API key in qwen_config.json.
 * If API calls fail, affected entities will have null portraits/descriptions but generation continues.
 * </p>
 *
 * @author mei1234567554
 * @version 4.0.0
 * @since 2026/4/7
 * @see PortraitGenerator
 * @see QwenConfiguration
 * @see JsonRepository
 */
@Slf4j
public class TestDataGenerator {

    private final JsonRepository<User> userRepo = new JsonRepository<>(User.class);
    private final JsonRepository<TAProfile> taProfileRepo = new JsonRepository<>(TAProfile.class);
    private final JsonRepository<MOProfile> moProfileRepo = new JsonRepository<>(MOProfile.class);
    private final JsonRepository<Position> positionRepo = new JsonRepository<>(Position.class);
    private final JsonRepository<Application> applicationRepo = new JsonRepository<>(Application.class);

    /** Directory path for storing generated resume files (relative to file root) */
    @Getter
    private String resumeDir;

    /** Qwen AI generation client instance */
    private Generation generation;

    /** Random number generator for creating varied test data */
    private final Random rd = new Random();

    /** Static flag to prevent duplicate data generation across multiple invocations */
    private static volatile boolean alreadyGenerated = false;

    /**
     * Private constructor initializes generator with configuration from ApplicationConfiguration.
     * <p>
     * Retrieves resume directory path from centralized configuration to ensure consistency
     * with runtime file storage locations.
     * </p>
     */
    private TestDataGenerator() {
        this.resumeDir = ApplicationConfiguration.getInstance().getFileDir();
        log.info("TestDataGenerator initialized with resumeDir (from config): {}", this.resumeDir);
    }

    /**
     * Entry point for test data generation with single-execution guard.
     * <p>
     * This static method ensures test data is generated only once per JVM lifecycle:
     * <ol>
     *   <li>Checks {@code alreadyGenerated} flag</li>
     *   <li>If already generated: Logs warning and returns immediately</li>
     *   <li>Otherwise: Creates generator instance and calls {@link #generate()}</li>
     *   <li>Sets flag to true on success</li>
     *   <li>Calls {@link #cleanup()} in finally block to release resources</li>
     * </ol>
     * </p>
     * <p>
     * <b>Thread Safety:</b> The {@code alreadyGenerated} flag is declared as {@code volatile}
     * to ensure visibility across threads in concurrent environments.
     * </p>
     * <p>
     * <b>Error Handling:</b> Wraps generation in try-catch-finally to ensure cleanup even
     * if generation fails. Re-throws as RuntimeException for caller handling.
     * </p>
     *
     * @throws RuntimeException if data generation fails (wraps underlying exception)
     */
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

    /**
     * Orchestrates the complete test data generation workflow.
     * <p>
     * This method executes the full generation pipeline in sequential and parallel phases:
     * </p>
     * <p>
     * <b>Phase 1 - Sequential Generation:</b>
     * <ol>
     *   <li>Initialize Qwen Generation client</li>
     *   <li>Generate 23 users (15 TAs + 8 MOs)</li>
     *   <li>Create basic TA profiles with template data</li>
     *   <li>Create basic MO profiles</li>
     *   <li>Generate 20 positions with random attributes</li>
     *   <li>Create random applications linking TAs to positions</li>
     * </ol>
     * </p>
     * <p>
     * <b>Phase 2 - Parallel AI Enhancement:</b>
     * <ul>
     *   <li>Thread 1: Generate TA resumes (AI) + vector portraits → updates TAProfile objects</li>
     *   <li>Thread 2: Generate position descriptions (AI) + vector portraits → updates Position objects</li>
     *   <li>Wait for both threads to complete using CountDownLatch</li>
     *   <li>Shutdown executor with 60-second timeout</li>
     * </ul>
     * </p>
     * <p>
     * <b>Phase 3 - Final Enhancement:</b>
     * <ol>
     *   <li>Generate application feedback (AI) for offered/rejected applications</li>
     <li>Save all entities to JSON files via {@link #saveAllData(List, List, List, List, List, List)} method</li>
     * </ol>
     * </p>
     * <p>
     * <b>Statistics Logging:</b> After completion, logs counts for all entity types:
     * Users, TA Profiles, MO Profiles, Positions, Applications, Portraits.
     * </p>
     *
     * @throws Exception if any generation step fails (wrapped in RuntimeException by caller)
     * @see #generateUsers()
     * @see #generateCompleteTAProfiles(List, List)
     * @see #generateCompletePositions(List, List)
     * @see #generateCompleteApplications(List, List, List)
     */
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

    /**
     * Releases Generation client resources after data generation completes.
     * <p>
     * Sets the {@code generation} field to null to allow garbage collection
     * of the DashScope API client and associated resources.
     * </p>
     */
    private void cleanup() {
        generation = null;
        log.debug("TestDataGenerator resources cleaned up");
    }

    /**
     * Generates user accounts for TAs and MOs with predefined names and hashed passwords.
     * <p>
     * Creates 23 user accounts:
     * <ul>
     *   <li><b>15 TA Users</b>: Names like "zhangsan", "lisi", "wangwu", etc.</li>
     *   <li><b>8 MO Users</b>: Names like "mo_chen", "mo_li", "mo_wang", etc.</li>
     * </ul>
     * </p>
     * <p>
     * <b>User Attributes:</b>
     * <ul>
     *   <li>Password: MD5 hash of "password123" (consistent for all test users)</li>
     *   <li>Role: 1 for TAs, 2 for MOs</li>
     *   <li>Status: 0 (active) for all users</li>
     *   <li>CreateAt: Staggered dates (60 days ago for first TA, decreasing by 4 days each)</li>
     *   <li>UpdateAt: Current timestamp</li>
     *   <li>LastLoginAt: Recent timestamps (1-10 hours ago for TAs, 2-9 hours ago for MOs)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Temporal Distribution:</b> CreateAt timestamps are spread over ~60 days to simulate
     * organic user registration patterns rather than bulk creation.
     * </p>
     *
     * @return List of 23 generated User entities (loaded from existing data + newly created)
     * @throws IOException if loading existing users from repository fails
     * @see #hashPassword(String)
     */
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

    /**
     * Generates basic TA profiles with template-based academic data.
     * <p>
     * Creates profiles for all TA users (role=1) with realistic academic information:
     * </p>
     * <p>
     * <b>Profile Fields:</b>
     * <ul>
     *   <li><b>Name</b>: Capitalized version of username (e.g., "zhangsan" → "Zhangsan")</li>
     *   <li><b>Gender</b>: Random "Male" or "Female"</li>
     *   <li><b>Age</b>: Random between 18-29 years old</li>
     *   <li><b>College</b>: One of 10 schools (Computer Science, Engineering, Business, etc.)</li>
     *   <li><b>Major</b>: One of 15 majors (CS, SE, IS, Data Science, ME, EE, etc.)</li>
     *   <li><b>Degree</b>: Random Bachelor/Master/PhD</li>
     *   <li><b>Year</b>: Academic year 1-4 (cycling through TA list)</li>
     *   <li><b>Skills</b>: One of 10 predefined skill sets matched to major</li>
     *   <li><b>Email</b>: username@bupt.edu.com format</li>
     *   <li><b>Phone</b>: Simulated Chinese mobile numbers (138xxxx xxxx pattern)</li>
     *   <li><b>Resume</b>: Path set to "resumes/resume_username.md" (relative path with subdirectory)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Skill Sets:</b> 10 diverse technical skill combinations:
     * <ol>
     *   <li>Java, Python, Spring Boot, MySQL (Backend Development)</li>
     *   <li>JavaScript, React, Node.js, MongoDB (Full Stack Web)</li>
     *   <li>C++, Algorithms, Data Structures, Linux (Systems Programming)</li>
     *   <li>Machine Learning, TensorFlow, Python, Statistics (AI/ML)</li>
     *   <li>Web Development, HTML/CSS, Vue.js, PostgreSQL (Frontend Focus)</li>
     *   <li>CAD, SolidWorks, MATLAB, Mechanical Design (Mechanical Eng)</li>
     *   <li>Circuit Design, Verilog, Embedded Systems, IoT (Electrical Eng)</li>
     *   <li>Structural Analysis, AutoCAD, Project Management, Construction (Civil Eng)</li>
     *   <li>Financial Modeling, Excel, Risk Analysis, Accounting (Finance)</li>
     *   <li>Legal Research, Contract Law, Critical Thinking, Writing (Law)</li>
     * </ol>
     * </p>
     * <p>
     * <b>Resume Path Convention:</b> Uses relative path with subdirectory ("resumes/")
     * consistent with production file upload behavior. Actual file content generated later
     * by {@link #generateCompleteTAProfiles(List, List)}.
     * </p>
     *
     * @param users List of all users (filtered internally for role=1)
     * @return List of TAProfile entities with basic academic data
     */
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

    /**
     * Generates basic MO profiles with minimal required information.
     * <p>
     * Creates profiles for all MO users (role=2) with essential contact details:
     * <ul>
     *   <li><b>Name</b>: Formatted from username (e.g., "mo_chen" → "Mo Chen")</li>
     *   <li><b>College</b>: Random selection from 3 schools (CS, Engineering, Business)</li>
     *   <li><b>Email</b>: username@bupt.edu.com format</li>
     *   <li><b>Phone</b>: Simulated Chinese mobile numbers (139xxxx xxxx pattern)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Note:</b> MO profiles are simpler than TA profiles as they don't require
     * resumes, skills, or academic history. MOs are faculty/staff members posting positions.
     * </p>
     *
     * @param users List of all users (filtered internally for role=2)
     * @return List of MOProfile entities with basic contact information
     */
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

    /**
     * Generates 20 teaching positions with varied attributes across departments.
     * <p>
     * Creates diverse position listings with realistic academic course information:
     * </p>
     * <p>
     * <b>Position Attributes:</b>
     * <ul>
     *   <li><b>Title</b>: One of 20 titles (e.g., "Teaching Assistant - Introduction to Programming")</li>
     *   <li><b>Module Code</b>: Course codes like CS101, CS201, ME201, BA301, etc.</li>
     *   <li><b>Module Name</b>: Full course names matching titles</li>
     *   <li><b>Skills</b>: Required technical skills (3-4 per position, matched to course)</li>
     *   <li><b>Post User</b>: Random active MO user (role=2, status=0)</li>
     *   <li><b>Weekly Workload</b>: Even numbers 2-16 hours (random * 2)</li>
     *   <li><b>Duration</b>: 1-12 weeks</li>
     *   <li><b>Required Num</b>: 1-6 TAs needed</li>
     *   <li><b>Start Date</b>: 2-5 weeks in the future</li>
     *   <li><b>End Date</b>: Calculated from start date + duration</li>
     *   <li><b>Deadline</b>: 5-12 days before start date</li>
     *   <li><b>Post Date</b>: 0-59 days ago (random within last 2 months)</li>
     *   <li><b>Min Grade</b>: Optional minimum grade requirement (randomly set ~50% of time)</li>
     *   <li><b>Max Grade</b>: Optional maximum grade cap (set if minGrade exists, ~50% of those)</li>
     *   <li><b>Status</b>: Random 0-3 (opened/closed/filled/withdrawn)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Course Distribution:</b> 20 positions covering:
     * <ul>
     *   <li>Computer Science: 10 courses (Programming, DS&A, Web Dev, DB, ML, SE, Networks, OS, Graphics, Discrete Math)</li>
     *   <li>Engineering: 2 courses (Mechanical Design, Circuit Analysis)</li>
     *   <li>Business: 2 courses (Accounting, Statistics)</li>
     *   <li>Science: 2 courses (Chemistry, Biology)</li>
     *   <li>Mathematics: 2 courses (Calculus, Linear Algebra)</li>
     *   <li>Humanities: 2 courses (Law, Literature)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Grade Requirements:</b> Min/Max grade fields are conditionally set to simulate
     * realistic scenarios where some positions have GPA restrictions while others don't.
     * </p>
     *
     * @param users List of all users (filtered internally for active MOs)
     * @return List of 20 Position entities with complete course information
     */
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

    /**
     * Generates random applications linking TA users to open positions.
     * <p>
     * Creates application records with varied statuses to simulate realistic recruitment activity:
     * <ol>
     *   <li>Filters positions to ~66% of total (random selection) AND status=0 (opened)</li>
     *   <li>For each selected position:
     *     <ul>
     *       <li>Random number of applications (1 to total TA count)</li>
     *       <li>Sequential TA assignment with wraparound (cycles through TA list)</li>
     *       <li>Random status: 0 (applied), 1 (offered), 2 (rejected), 3 (withdrawn)</li>
     *       <li>ApplyAt: Random within last 30 days</li>
     *       <li>Updates position statistics (appliedNum, offeredNum, rejectedNum)</li>
     *     </ul>
     *   </li>
     * </ol>
     * </p>
     * <p>
     * <b>Status Distribution:</b> Equal probability (25% each) for:
     * <ul>
     *   <li>Status 0: Applied - Awaiting review</li>
     *   <li>Status 1: Offered - TA received offer</li>
     *   <li>Status 2: Rejected - Application declined</li>
     *   <li>Status 3: Withdrawn - TA withdrew application</li>
     * </ul>
     * </p>
     * <p>
     * <b>Position Statistics:</b> Automatically maintains accurate counts:
     * <ul>
     *   <li>appliedNum: Incremented for all non-withdrawn applications</li>
     *   <li>offeredNum: Incremented when status=1</li>
     *   <li>rejectedNum: Incremented when status=2</li>
     * </ul>
     * Note: Withdrawn applications (status=3) don't increment appliedNum.
     * </p>
     *
     * @param positions List of all positions (filtered for opened ones)
     * @param users     List of all users (filtered for active TAs)
     * @return List of Application entities with random assignments and statuses
     */
    private List<Application> generateBasicApplications(List<Position> positions, List<User> users) {
        log.info("Generating applications...");
        List<Application> applications = new ArrayList<>();

        List<Position> openPositions = positions.stream()
                .filter(p -> rd.nextInt(3) < 2)
                .filter(p -> p.getStatus() == 0)
                .toList();

        List<User> taUsers = users.stream().filter(u -> u.getRole() == 1 && u.getStatus() == 0).toList();

        for (Position position : openPositions) {
            int numApplications = rd.nextInt(1, taUsers.size());
            for (int i = 0, userIdx = rd.nextInt(taUsers.size()); i < numApplications; i++, userIdx++) {
                if (userIdx >= taUsers.size()) {
                    userIdx = 0;
                }

                Application application = new Application();
                application.setPositionId(position.getId());
                application.setUserId(taUsers.get(userIdx).getId());
                application.setStatus(rd.nextInt(4));
                application.setApplyAt(Timestamp.valueOf(LocalDateTime.now().minusDays(rd.nextInt(30))));
                if (application.getStatus() != 3) {
                    position.setAppliedNum(position.getAppliedNum() + 1);
                }
                switch (application.getStatus()) {
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

    /**
     * Generates AI-enhanced TA profiles with Markdown resumes and vector portraits.
     * <p>
     * This method runs in a parallel thread and performs dual AI operations for each TA:
     * <ol>
     *   <li><b>Resume Generation</b>: Calls Qwen AI to create 300-400 word Markdown resume with:
     *     <ul>
     *       <li>Education section (college, major, degree, year)</li>
     *       <li>Technical Skills section (from profile.skills)</li>
     *       <li>Project Experience (2-3 realistic projects for the major)</li>
     *       <li>Achievements section</li>
     *     </ul>
     *   </li>
     *   <li><b>File Storage</b>: Writes resume to disk at absolute path with subdirectory:
     *     <ul>
     *       <li>Creates parent directories if they don't exist</li>
     *       <li>Validates file was created successfully (exists and size > 0)</li>
     *     </ul>
     *   </li>
     *   <li><b>Portrait Generation</b>: Calls {@link PortraitGenerator#generatePortrait(TAProfile, File)} to:
     *     <ul>
     *       <li>Parse resume content</li>
     *       <li>Extract skills, experience, education vectors</li>
     *       <li>Combine into unified portrait embedding</li>
     *     </ul>
     *   </li>
     *   <li><b>Profile Update</b>: Sets portraitId on TAProfile object</li>
     * </ol>
     * </p>
     * <p>
     * <b>Parallel Execution:</b> Runs concurrently with {@link #generateCompletePositions(List, List)}
     * using a fixed thread pool of 2 threads. Results collected in synchronized list.
     * </p>
     * <p>
     * <b>Error Handling:</b> Individual TA failures are logged but don't abort generation.
     * Failed TAs return null and are filtered out before adding to portraits list.
     * Success rate logged at completion (e.g., "14/15 successful").
     * </p>
     * <p>
     * <b>File Path Strategy:</b>
     * <ul>
     *   <li>Profile stores relative path: "resumes/resume_zhangsan.md"</li>
     *   <li>Generation uses absolute path: ApplicationConfiguration.getFilePath() + relative path</li>
     *   <li>This ensures files are written to correct location regardless of working directory</li>
     * </ul>
     * </p>
     *
     * @param profiles  List of TAProfile entities to enhance (modified in-place with portraitId)
     * @param portraits Synchronized list to collect generated Portrait entities (thread-safe)
     * @see #generateResumeMarkdown(String, String, String, int, String, List, String, String)
     * @see PortraitGenerator#generatePortrait(TAProfile, File)
     */
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

    /**
     * Generates AI-enhanced positions with descriptions and vector portraits.
     * <p>
     * This method runs in a parallel thread and performs dual AI operations for each position:
     * <ol>
     *   <li><b>Description Generation</b>: Calls Qwen AI to create 60-100 word professional job description:
     *     <ul>
     *       <li>Includes key responsibilities</li>
     *       <li>Mentions required skills naturally</li>
     *       <li>Uses professional academic tone</li>
     *       <li>Starts with action verbs</li>
     *     </ul>
     *   </li>
     *   <li><b>Position Update</b>: Sets description field on Position object</li>
     *   <li><b>Portrait Generation</b>: Calls {@link PortraitGenerator#generatePortrait(Position)} to:
     *     <ul>
     *       <li>Extract required skills vector</li>
     *       <li>Analyze description for responsibility keywords</li>
     *       <li>Combine into unified position portrait embedding</li>
     *     </ul>
     *   </li>
     *   <li><b>Position Update</b>: Sets portraitId on Position object</li>
     * </ol>
     * </p>
     * <p>
     * <b>Parallel Execution:</b> Runs concurrently with {@link #generateCompleteTAProfiles(List, List)}
     * using a fixed thread pool of 2 threads. Results collected in synchronized list.
     * </p>
     * <p>
     * <b>Error Handling:</b> Individual position failures are logged but don't abort generation.
     * Failed positions return null and are filtered out before adding to portraits list.
     * Success rate logged at completion (e.g., "19/20 successful").
     * </p>
     *
     * @param positions List of Position entities to enhance (modified in-place with description and portraitId)
     * @param portraits Synchronized list to collect generated Portrait entities (thread-safe)
     * @see #generatePositionDescription(String, String, List)
     * @see PortraitGenerator#generatePortrait(Position)
     */
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

    /**
     * Generates professional job description for a position using Qwen AI.
     * <p>
     * Constructs a prompt requesting a 60-100 word academic job description with:
     * <ul>
     *   <li>Key responsibilities for the TA role</li>
     *   <li>Natural integration of required skills</li>
     *   <li>Professional academic tone</li>
     *   <li>Action verb openings</li>
     * </ul>
     * </p>
     * <p>
     * <b>AI Configuration:</b>
     * <ul>
     *   <li>Model: Configured in qwen_config.json (typically qwen-turbo or qwen-plus)</li>
     *   <li>Temperature: 0.7 (balanced creativity and coherence)</li>
     *   <li>Max Tokens: 300 (sufficient for 60-100 words)</li>
     *   <li>System Prompt: "You are a professional academic job description writer."</li>
     * </ul>
     * </p>
     * <p>
     * <b>Prompt Template:</b>
     * <pre>
     * Generate a professional job description for a Teaching Assistant position.
     * 
     * Position Title: {title}
     * Module Name: {moduleName}
     * Required Skills: {skills joined by comma}
     * 
     * Requirements:
     * - 60-100 words
     * - Include key responsibilities
     * - Mention required skills naturally
     * - Professional academic tone
     * - Start with action verbs
     * 
     * Return ONLY the description text, no JSON formatting.
     * </pre>
     * </p>
     *
     * @param title      Position title (e.g., "Teaching Assistant - Introduction to Programming")
     * @param moduleName Course name (e.g., "Introduction to Programming")
     * @param skills     List of required technical skills
     * @return Generated description text (trimmed), or throws exception on API failure
     * @throws Exception if Qwen API call fails or returns invalid response
     * @see QwenConfiguration#getApiKey()
     */
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


    /**
     * Generates AI-powered feedback messages for offered and rejected applications.
     * <p>
     * This method processes applications with status=1 (offered) or status=2 (rejected)
     * and generates personalized feedback using Qwen AI:
     * <ol>
     *   <li>Filters applications to only those with status 1 or 2</li>
     *   <li>Builds lookup maps:
     *     <ul>
     *       <li>positionMap: positionId → Position object</li>
     *       <li>applicantNameMap: userId → TA name (from TAProfile)</li>
     *     </ul>
     *   </li>
     *   <li>Processes applications in parallel stream:
     *     <ul>
     *       <li>Retrieves position and applicant name</li>
     *       <li>Calls {@link #generateApplicationFeedback(int, Position, String)}</li>
     *       <li>Sets feedback field on Application object</li>
     *       <li>Adds to success list</li>
     *     </ul>
     *   </li>
     *   <li>Logs success rate (e.g., "45/50 successful")</li>
     * </ol>
     * </p>
     * <p>
     * <b>Feedback Types:</b>
     * <ul>
     *   <li><b>Offer Feedback (status=1)</b>: Positive message highlighting strengths, fit for position, next steps</li>
     *   <li><b>Rejection Feedback (status=2)</b>: Constructive message mentioning areas for improvement, encouraging future applications</li>
     * </ul>
     * Both types are 30-50 words with professional HR tone.
     * </p>
     * <p>
     * <b>Personalization:</b> Each feedback includes:
     * <ul>
     *   <li>Position title and module code</li>
     *   <li>Applicant name (from TAProfile)</li>
     *   <li>Context-appropriate tone (encouraging for offers, constructive for rejections)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Error Handling:</b> Individual application failures are logged but don't abort generation.
     * Failed applications simply don't get feedback set (remains null).
     * </p>
     *
     * @param applications List of all applications (filtered internally for status 1/2)
     * @param positions    List of all positions (for lookup by positionId)
     * @param taProfiles   List of all TA profiles (for applicant name lookup)
     * @see #generateApplicationFeedback(int, Position, String)
     */
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

    /**
     * Generates personalized offer or rejection feedback using Qwen AI.
     * <p>
     * Constructs conditional prompts based on application status:
     * </p>
     * <p>
     * <b>Offer Feedback (status=1):</b>
     * <pre>
     * Write a positive offer feedback for a TA application.
     * 
     * Position: {title} ({moduleCode})
     * Applicant: {applicantName}
     * 
     * Requirements:
     * - 30-50 words
     * - Highlight strengths and fit for the position
     * - Professional and encouraging tone
     * - Mention next steps briefly
     * - Return ONLY the feedback text.
     * </pre>
     * </p>
     * <p>
     * <b>Rejection Feedback (status=2):</b>
     * <pre>
     * Write a rejection feedback for a TA application.
     * 
     * Position: {title} ({moduleCode})
     * Applicant: {applicantName}
     * 
     * Requirements:
     * - 30-50 words
     * - Constructive and polite
     * - Mention areas for improvement or missing qualifications
     * - Encourage future applications
     * - Return ONLY the feedback text.
     * </pre>
     * </p>
     * <p>
     * <b>AI Configuration:</b>
     * <ul>
     *   <li>Model: Configured in qwen_config.json</li>
     *   <li>Temperature: 0.7 (balanced tone)</li>
     *   <li>Max Tokens: 200 (sufficient for 30-50 words)</li>
     *   <li>System Prompt: "You are a professional HR manager providing application feedback."</li>
     * </ul>
     * </p>
     *
     * @param status         Application status (1=offered, 2=rejected)
     * @param position       Position entity for context (title, moduleCode)
     * @param applicantName  TA's name for personalization
     * @return Generated feedback text (trimmed), or throws exception on API failure
     * @throws Exception if Qwen API call fails or returns invalid response
     */
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

    /**
     * Generates professional Markdown resume for a TA candidate using Qwen AI.
     * <p>
     * Creates a 300-400 word resume in standard Markdown format with sections:
     * <ul>
     *   <li><b>Education</b>: College, major, degree, year</li>
     *   <li><b>Technical Skills</b>: Formatted list from skills parameter</li>
     *   <li><b>Project Experience</b>: 2-3 realistic projects appropriate for major and year level</li>
     *   <li><b>Achievements</b>: Academic honors, certifications, relevant accomplishments</li>
     * </ul>
     * </p>
     * <p>
     * <b>AI Configuration:</b>
     * <ul>
     *   <li>Model: qwen-long (configured for longer content generation)</li>
     *   <li>Temperature: 0.7 (balanced creativity)</li>
     *   <li>Max Tokens: 800 (sufficient for 300-400 words)</li>
     *   <li>System Prompt: "You are a professional resume writer for university students."</li>
     * </ul>
     * </p>
     * <p>
     * <b>Prompt Template:</b>
     * <pre>
     * Generate a professional resume in Markdown format for a university student applying for a Teaching Assistant position.
     * 
     * Name: {name}
     * Major: {major}
     * Degree: {degree} (Year {year})
     * College: {college}
     * Skills: {skills joined by comma}
     * Email: {email}
     * Phone: {phone}
     * 
     * Requirements:
     * - Use standard Markdown formatting
     * - Include sections: Education, Technical Skills, Project Experience (2-3 projects), Achievements
     * - Projects should be realistic for the major and year
     * - 300-400 words total
     * - Professional academic tone
     * - Return ONLY the Markdown content, no explanations
     * </pre>
     * </p>
     * <p>
     * <b>Content Realism:</b> AI generates projects appropriate for the student's:
     * <ul>
     *   <li>Major (CS projects vs ME projects vs Business projects, etc.)</li>
     *   <li>Year level (simpler projects for Year 1-2, complex for Year 3-4)</li>
     *   <li>Degree level (Bachelor vs Master vs PhD complexity)</li>
     * </ul>
     * </p>
     *
     * @param name    Student's full name
     * @param major   Academic major (e.g., "Computer Science")
     * @param degree  Degree level (Bachelor/Master/PhD)
     * @param year    Academic year (1-4)
     * @param college School/college name
     * @param skills  List of technical skills to include
     * @param email   Contact email address
     * @param phone   Contact phone number
     * @return Generated Markdown resume content (trimmed), or throws exception on API failure
     * @throws Exception if Qwen API call fails or returns invalid response
     * @see QwenConfiguration#getQwenLong()
     */
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

    /**
     * Saves all generated entities to JSON files in the data/ directory.
     * <p>
     * Persists each entity type to its corresponding JSON file:
     * <ul>
     *   <li>user.json - All user accounts (TAs + MOs)</li>
     *   <li>ta_profile.json - All TA profiles with portraitIds</li>
     *   <li>mo_profile.json - All MO profiles</li>
     *   <li>position.json - All positions with descriptions and portraitIds</li>
     *   <li>application.json - All applications with feedback messages</li>
     *   <li>portrait.json - All vector portraits (TA + Position)</li>
     * </ul>
     * </p>
     * <p>
     * <b>File Format:</b> JSON arrays serialized by {@link JsonRepository#saveAllEntities(List)}.
     * Each file contains a complete snapshot of that entity type.
     * </p>
     * <p>
     * <b>Logging:</b> Logs count of entities saved for each file type for verification.
     * </p>
     *
     * @param users        List of User entities to save
     * @param taProfiles   List of TAProfile entities to save
     * @param moProfiles   List of MOProfile entities to save
     * @param positions    List of Position entities to save
     * @param applications List of Application entities to save
     * @param portraits    List of Portrait entities to save
     * @throws IOException if file writing fails for any entity type
     */
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

    /**
     * Hashes a password string using MD5 algorithm.
     * <p>
     * Wraps Apache Commons Codec's {@link DigestUtils#md5Hex(String)} to provide
     * consistent password hashing across the application.
     * </p>
     * <p>
     * <b>Usage in Test Data:</b> All test users receive the same password hash
     * (MD5 of "password123") for simplicity. In production, each user would have
     * unique passwords.
     * </p>
     *
     * @param password Plain text password to hash
     * @return MD5-hashed password string (32-character hexadecimal)
     * @see DigestUtils#md5Hex(String)
     */
    private String hashPassword(String password) {
        return DigestUtils.md5Hex(password);
    }
}

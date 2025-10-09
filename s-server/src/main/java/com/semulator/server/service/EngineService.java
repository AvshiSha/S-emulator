package com.semulator.server.service;

import com.semulator.engine.api.SEngine;
import com.semulator.engine.model.*;
import com.semulator.engine.parse.SProgramImpl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton service that wraps the S-Engine core
 * Manages programs, runs, users, and credits in-memory
 */
public class EngineService {

    private static final EngineService INSTANCE = new EngineService();

    // In-memory storage
    private final Map<String, SProgram> programs = new ConcurrentHashMap<>();
    private final Map<String, Integer> userCredits = new ConcurrentHashMap<>();
    private final Map<String, List<RunRecord>> runHistory = new ConcurrentHashMap<>();

    private EngineService() {
        // Initialize with default admin user
        userCredits.put("admin", 1000);
    }

    public static EngineService getInstance() {
        return INSTANCE;
    }

    /**
     * Get catalog of all loaded programs and functions
     */
    public SEngine.CatalogSummary getCatalog() {
        List<SEngine.ProgramInfo> programInfos = new ArrayList<>();
        Map<String, SEngine.FunctionInfo> functionInfos = new HashMap<>();
        int totalInstructions = 0;

        for (Map.Entry<String, SProgram> entry : programs.entrySet()) {
            SProgram prog = entry.getValue();
            int instructionCount = prog.getInstructions().size();
            totalInstructions += instructionCount;

            programInfos.add(new SEngine.ProgramInfo(
                    prog.getName(),
                    instructionCount,
                    prog.calculateMaxDegree(),
                    new ArrayList<>() // TODO: Extract function usage
            ));

            // Extract functions if SProgramImpl
            if (prog instanceof SProgramImpl) {
                SProgramImpl impl = (SProgramImpl) prog;
                Map<String, String> userStrings = impl.getFunctionUserStrings();

                for (String funcName : impl.getFunctions().keySet()) {
                    functionInfos.put(funcName, new SEngine.FunctionInfo(
                            funcName,
                            userStrings.getOrDefault(funcName, ""),
                            prog.calculateFunctionTemplateDegree(funcName),
                            impl.getFunctions().get(funcName).size()));
                }
            }
        }

        return new SEngine.CatalogSummary(programInfos, functionInfos, totalInstructions);
    }

    /**
     * Load a program from XML content
     */
    public SEngine.LoadResult loadProgram(String xmlPath, String ownerUsername) {
        try {
            SProgramImpl program = new SProgramImpl("LoadedProgram");
            String validation = program.validate(java.nio.file.Path.of(xmlPath));

            if (!"Valid".equals(validation)) {
                return new SEngine.LoadResult(false, null, validation);
            }

            program.load();
            programs.put(program.getName(), program);

            return new SEngine.LoadResult(true, program.getName(), null);
        } catch (Exception e) {
            return new SEngine.LoadResult(false, null, e.getMessage());
        }
    }

    /**
     * Get user credits
     */
    public int getUserCredits(String username) {
        return userCredits.getOrDefault(username, 0);
    }

    /**
     * Deduct credits from user
     */
    public boolean deductCredits(String username, int amount) {
        int current = getUserCredits(username);
        if (current >= amount) {
            userCredits.put(username, current - amount);
            return true;
        }
        return false;
    }

    static class RunRecord {
        final String programName;
        final long result;
        final int cycles;
        final long timestamp;

        RunRecord(String programName, long result, int cycles) {
            this.programName = programName;
            this.result = result;
            this.cycles = cycles;
            this.timestamp = System.currentTimeMillis();
        }
    }
}

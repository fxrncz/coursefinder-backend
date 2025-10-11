package com.app.services;

import com.app.dto.DetailedScoringDTO;
import com.app.models.PersonalityTestScores;
import com.app.repositories.PersonalityTestScoresRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class EnhancedScoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedScoringService.class);
    
    @Autowired
    private PersonalityTestScoresRepository scoringRepository;
    
    // RIASEC and MBTI descriptions for better visualization
    private static final Map<String, String> RIASEC_DESCRIPTIONS = Map.of(
        "R", "Realistic - Practical, hands-on, mechanical",
        "I", "Investigative - Analytical, scientific, intellectual",
        "A", "Artistic - Creative, expressive, original",
        "S", "Social - Helpful, cooperative, caring",
        "E", "Enterprising - Leadership, persuasive, ambitious",
        "C", "Conventional - Organized, detail-oriented, systematic"
    );
    
    private static final Map<String, String> MBTI_DESCRIPTIONS = Map.of(
        "E", "Extraversion - Outgoing, social, energetic",
        "I", "Introversion - Reflective, reserved, focused",
        "S", "Sensing - Practical, concrete, detail-oriented",
        "N", "Intuition - Abstract, theoretical, future-focused",
        "T", "Thinking - Logical, objective, analytical",
        "F", "Feeling - Values-based, empathetic, personal",
        "J", "Judging - Structured, decisive, organized",
        "P", "Perceiving - Flexible, adaptable, spontaneous"
    );
    
    /**
     * Calculate enhanced RIASEC scores with accurate percentages
     */
    public Map<String, DetailedScoringDTO.ScoreData> calculateEnhancedRIASECScores(Map<Integer, Integer> answers) {
        Map<String, DetailedScoringDTO.ScoreData> scores = new HashMap<>();
        
        // Calculate raw scores for each RIASEC dimension (10 questions each, Likert 1-7)
        int rRaw = sumAnswers(answers, 0, 9);    // Questions 0-9
        int iRaw = sumAnswers(answers, 10, 19);  // Questions 10-19
        int aRaw = sumAnswers(answers, 20, 29);  // Questions 20-29
        int sRaw = sumAnswers(answers, 30, 39);  // Questions 30-39
        int eRaw = sumAnswers(answers, 40, 49);  // Questions 40-49
        int cRaw = sumAnswers(answers, 50, 59);  // Questions 50-59
        
        // Calculate percentages (max possible = 10 questions * 7 = 70)
        double rPercentage = calculatePercentage(rRaw, 70);
        double iPercentage = calculatePercentage(iRaw, 70);
        double aPercentage = calculatePercentage(aRaw, 70);
        double sPercentage = calculatePercentage(sRaw, 70);
        double ePercentage = calculatePercentage(eRaw, 70);
        double cPercentage = calculatePercentage(cRaw, 70);
        
        scores.put("R", new DetailedScoringDTO.ScoreData(rRaw, rPercentage, "Realistic", RIASEC_DESCRIPTIONS.get("R")));
        scores.put("I", new DetailedScoringDTO.ScoreData(iRaw, iPercentage, "Investigative", RIASEC_DESCRIPTIONS.get("I")));
        scores.put("A", new DetailedScoringDTO.ScoreData(aRaw, aPercentage, "Artistic", RIASEC_DESCRIPTIONS.get("A")));
        scores.put("S", new DetailedScoringDTO.ScoreData(sRaw, sPercentage, "Social", RIASEC_DESCRIPTIONS.get("S")));
        scores.put("E", new DetailedScoringDTO.ScoreData(eRaw, ePercentage, "Enterprising", RIASEC_DESCRIPTIONS.get("E")));
        scores.put("C", new DetailedScoringDTO.ScoreData(cRaw, cPercentage, "Conventional", RIASEC_DESCRIPTIONS.get("C")));
        
        logger.debug("Enhanced RIASEC scores calculated: R={}% ({}), I={}% ({}), A={}% ({}), S={}% ({}), E={}% ({}), C={}% ({})", 
                    rPercentage, rRaw, iPercentage, iRaw, aPercentage, aRaw, sPercentage, sRaw, ePercentage, eRaw, cPercentage, cRaw);
        
        return scores;
    }
    
    /**
     * Calculate enhanced MBTI scores with accurate percentages
     */
    public Map<String, DetailedScoringDTO.ScoreData> calculateEnhancedMBTIScores(Map<Integer, Integer> answers) {
        Map<String, DetailedScoringDTO.ScoreData> scores = new HashMap<>();
        
        // Calculate raw scores for each MBTI dimension (5 questions each, Likert 1-7)
        int eRaw = sumAnswers(answers, 60, 64);  // Questions 60-64
        int iRaw = sumAnswers(answers, 65, 69);  // Questions 65-69
        int sRaw = sumAnswers(answers, 70, 74);  // Questions 70-74
        int nRaw = sumAnswers(answers, 75, 79);  // Questions 75-79
        int tRaw = sumAnswers(answers, 80, 84);  // Questions 80-84
        int fRaw = sumAnswers(answers, 85, 89);  // Questions 85-89
        int jRaw = sumAnswers(answers, 90, 94);  // Questions 90-94
        int pRaw = sumAnswers(answers, 95, 99);  // Questions 95-99
        
        // Calculate percentages (max possible = 5 questions * 7 = 35)
        double ePercentage = calculatePercentage(eRaw, 35);
        double iPercentage = calculatePercentage(iRaw, 35);
        double sPercentage = calculatePercentage(sRaw, 35);
        double nPercentage = calculatePercentage(nRaw, 35);
        double tPercentage = calculatePercentage(tRaw, 35);
        double fPercentage = calculatePercentage(fRaw, 35);
        double jPercentage = calculatePercentage(jRaw, 35);
        double pPercentage = calculatePercentage(pRaw, 35);
        
        scores.put("E", new DetailedScoringDTO.ScoreData(eRaw, ePercentage, "Extraversion", MBTI_DESCRIPTIONS.get("E")));
        scores.put("I", new DetailedScoringDTO.ScoreData(iRaw, iPercentage, "Introversion", MBTI_DESCRIPTIONS.get("I")));
        scores.put("S", new DetailedScoringDTO.ScoreData(sRaw, sPercentage, "Sensing", MBTI_DESCRIPTIONS.get("S")));
        scores.put("N", new DetailedScoringDTO.ScoreData(nRaw, nPercentage, "Intuition", MBTI_DESCRIPTIONS.get("N")));
        scores.put("T", new DetailedScoringDTO.ScoreData(tRaw, tPercentage, "Thinking", MBTI_DESCRIPTIONS.get("T")));
        scores.put("F", new DetailedScoringDTO.ScoreData(fRaw, fPercentage, "Feeling", MBTI_DESCRIPTIONS.get("F")));
        scores.put("J", new DetailedScoringDTO.ScoreData(jRaw, jPercentage, "Judging", MBTI_DESCRIPTIONS.get("J")));
        scores.put("P", new DetailedScoringDTO.ScoreData(pRaw, pPercentage, "Perceiving", MBTI_DESCRIPTIONS.get("P")));
        
        logger.debug("Enhanced MBTI scores calculated: E={}% ({}), I={}% ({}), S={}% ({}), N={}% ({}), T={}% ({}), F={}% ({}), J={}% ({}), P={}% ({})", 
                    ePercentage, eRaw, iPercentage, iRaw, sPercentage, sRaw, nPercentage, nRaw, tPercentage, tRaw, fPercentage, fRaw, jPercentage, jRaw, pPercentage, pRaw);
        
        return scores;
    }
    
    /**
     * Determine final RIASEC code from enhanced scores
     */
    public String determineFinalRIASECCode(Map<String, DetailedScoringDTO.ScoreData> riasecScores) {
        // Sort by percentage descending for accurate ranking
        List<Map.Entry<String, DetailedScoringDTO.ScoreData>> sortedScores = new ArrayList<>(riasecScores.entrySet());
        sortedScores.sort((a, b) -> Double.compare(b.getValue().getPercentage(), a.getValue().getPercentage()));
        
        // Take top 2 in percentage order (highest first)
        String top1 = sortedScores.get(0).getKey();
        String top2 = sortedScores.get(1).getKey();
        
        // Return in percentage order (highest percentage first)
        return top1 + top2;
    }
    
    /**
     * Determine final MBTI type from enhanced scores
     */
    public String determineFinalMBTIType(Map<String, DetailedScoringDTO.ScoreData> mbtiScores) {
        StringBuilder mbtiType = new StringBuilder();
        
        // E vs I - compare percentages for accuracy
        double eScore = mbtiScores.get("E").getPercentage();
        double iScore = mbtiScores.get("I").getPercentage();
        mbtiType.append(eScore > iScore ? "E" : "I");
        
        // S vs N
        double sScore = mbtiScores.get("S").getPercentage();
        double nScore = mbtiScores.get("N").getPercentage();
        mbtiType.append(sScore > nScore ? "S" : "N");
        
        // T vs F
        double tScore = mbtiScores.get("T").getPercentage();
        double fScore = mbtiScores.get("F").getPercentage();
        mbtiType.append(tScore > fScore ? "T" : "F");
        
        // J vs P
        double jScore = mbtiScores.get("J").getPercentage();
        double pScore = mbtiScores.get("P").getPercentage();
        mbtiType.append(jScore > pScore ? "J" : "P");
        
        return mbtiType.toString();
    }
    
    /**
     * Save detailed scoring data to database
     */
    public PersonalityTestScores saveDetailedScores(Long testResultId, UUID sessionId, 
                                                   Map<String, DetailedScoringDTO.ScoreData> riasecScores,
                                                   Map<String, DetailedScoringDTO.ScoreData> mbtiScores,
                                                   String finalRiasecCode, String finalMbtiType) {
        
        logger.info("Saving detailed scoring data for testResultId: {}, sessionId: {}", testResultId, sessionId);
        logger.info("RIASEC scores: {}", riasecScores.keySet());
        logger.info("MBTI scores: {}", mbtiScores.keySet());
        
        PersonalityTestScores scores = new PersonalityTestScores();
        scores.setTestResultId(testResultId);
        scores.setSessionId(sessionId);
        
        // Set RIASEC scores (both raw and percentages)
        scores.setRiasecRRaw(riasecScores.get("R").getRaw());
        scores.setRiasecRPercentage(BigDecimal.valueOf(riasecScores.get("R").getPercentage()));
        scores.setRiasecIRaw(riasecScores.get("I").getRaw());
        scores.setRiasecIPercentage(BigDecimal.valueOf(riasecScores.get("I").getPercentage()));
        scores.setRiasecARaw(riasecScores.get("A").getRaw());
        scores.setRiasecAPercentage(BigDecimal.valueOf(riasecScores.get("A").getPercentage()));
        scores.setRiasecSRaw(riasecScores.get("S").getRaw());
        scores.setRiasecSPercentage(BigDecimal.valueOf(riasecScores.get("S").getPercentage()));
        scores.setRiasecERaw(riasecScores.get("E").getRaw());
        scores.setRiasecEPercentage(BigDecimal.valueOf(riasecScores.get("E").getPercentage()));
        scores.setRiasecCRaw(riasecScores.get("C").getRaw());
        scores.setRiasecCPercentage(BigDecimal.valueOf(riasecScores.get("C").getPercentage()));
        
        // Set MBTI scores (both raw and percentages)
        scores.setMbtiERaw(mbtiScores.get("E").getRaw());
        scores.setMbtiEPercentage(BigDecimal.valueOf(mbtiScores.get("E").getPercentage()));
        scores.setMbtiIRaw(mbtiScores.get("I").getRaw());
        scores.setMbtiIPercentage(BigDecimal.valueOf(mbtiScores.get("I").getPercentage()));
        scores.setMbtiSRaw(mbtiScores.get("S").getRaw());
        scores.setMbtiSPercentage(BigDecimal.valueOf(mbtiScores.get("S").getPercentage()));
        scores.setMbtiNRaw(mbtiScores.get("N").getRaw());
        scores.setMbtiNPercentage(BigDecimal.valueOf(mbtiScores.get("N").getPercentage()));
        scores.setMbtiTRaw(mbtiScores.get("T").getRaw());
        scores.setMbtiTPercentage(BigDecimal.valueOf(mbtiScores.get("T").getPercentage()));
        scores.setMbtiFRaw(mbtiScores.get("F").getRaw());
        scores.setMbtiFPercentage(BigDecimal.valueOf(mbtiScores.get("F").getPercentage()));
        scores.setMbtiJRaw(mbtiScores.get("J").getRaw());
        scores.setMbtiJPercentage(BigDecimal.valueOf(mbtiScores.get("J").getPercentage()));
        scores.setMbtiPRaw(mbtiScores.get("P").getRaw());
        scores.setMbtiPPercentage(BigDecimal.valueOf(mbtiScores.get("P").getPercentage()));
        
        // Set final results
        scores.setFinalRiasecCode(finalRiasecCode);
        scores.setFinalMbtiType(finalMbtiType);
        
        PersonalityTestScores savedScores = scoringRepository.save(scores);
        logger.info("Successfully saved detailed scoring data with ID: {}", savedScores.getId());
        logger.info("Saved RIASEC R: raw={}, percentage={}", savedScores.getRiasecRRaw(), savedScores.getRiasecRPercentage());
        logger.info("Saved MBTI E: raw={}, percentage={}", savedScores.getMbtiERaw(), savedScores.getMbtiEPercentage());
        
        return savedScores;
    }
    
    /**
     * Get detailed scoring data for visualization by test result ID
     */
    public DetailedScoringDTO getDetailedScoringDataByTestResultId(Long testResultId) {
        logger.info("Looking for detailed scoring data for test result ID: {}", testResultId);
        
        // Check if any data exists in the table
        long totalCount = scoringRepository.count();
        logger.info("Total personality test scores in database: {}", totalCount);
        
        Optional<PersonalityTestScores> scoresOpt = scoringRepository.findByTestResultId(testResultId);
        if (scoresOpt.isEmpty()) {
            logger.warn("No detailed scoring data found for test result ID: {}", testResultId);
            logger.warn("Available test result IDs in database:");
            // List all available test result IDs for debugging
            scoringRepository.findAll().forEach(score -> {
                logger.warn("Available test result ID: {}", score.getTestResultId());
            });
            return null;
        }
        
        PersonalityTestScores scores = scoresOpt.get();
        logger.info("Found detailed scoring data for test result ID: {}", testResultId);
        logger.info("RIASEC R: raw={}, percentage={}", scores.getRiasecRRaw(), scores.getRiasecRPercentage());
        logger.info("MBTI E: raw={}, percentage={}", scores.getMbtiERaw(), scores.getMbtiEPercentage());
        
        return buildDetailedScoringDTO(scores);
    }
    
    /**
     * Get detailed scoring data for visualization by session ID (legacy method)
     */
    public DetailedScoringDTO getDetailedScoringData(UUID sessionId) {
        logger.info("Looking for detailed scoring data for session: {}", sessionId);
        logger.info("Session ID type: {}, value: {}", sessionId.getClass().getSimpleName(), sessionId.toString());
        
        // Check if any data exists in the table
        long totalCount = scoringRepository.count();
        logger.info("Total personality test scores in database: {}", totalCount);
        
        Optional<PersonalityTestScores> scoresOpt = scoringRepository.findBySessionId(sessionId);
        if (scoresOpt.isEmpty()) {
            logger.warn("No detailed scoring data found for session: {}", sessionId);
            logger.warn("Available sessions in database:");
            // List all available sessions for debugging
            scoringRepository.findAll().forEach(score -> {
                logger.warn("Available session: {}", score.getSessionId());
            });
            return null;
        }
        
        PersonalityTestScores scores = scoresOpt.get();
        logger.info("Found detailed scoring data for session: {}", sessionId);
        logger.info("RIASEC R: raw={}, percentage={}", scores.getRiasecRRaw(), scores.getRiasecRPercentage());
        logger.info("MBTI E: raw={}, percentage={}", scores.getMbtiERaw(), scores.getMbtiEPercentage());
        
        return buildDetailedScoringDTO(scores);
    }
    
    /**
     * Generate graph data for RIASEC visualization
     */
    private Map<String, Object> generateRIASECGraphData(Map<String, DetailedScoringDTO.ScoreData> scores) {
        Map<String, Object> graphData = new HashMap<>();
        
        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, DetailedScoringDTO.ScoreData> entry : scores.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("dimension", entry.getKey());
            item.put("label", entry.getValue().getLabel());
            item.put("raw", entry.getValue().getRaw());
            item.put("percentage", entry.getValue().getPercentage());
            item.put("description", entry.getValue().getDescription());
            data.add(item);
        }
        
        graphData.put("data", data);
        graphData.put("type", "riasec");
        graphData.put("title", "RIASEC Personality Dimensions");
        graphData.put("maxValue", 70);
        
        return graphData;
    }
    
    /**
     * Generate graph data for MBTI visualization
     */
    private Map<String, Object> generateMBTIGraphData(Map<String, DetailedScoringDTO.ScoreData> scores) {
        Map<String, Object> graphData = new HashMap<>();
        
        // Group by pairs for comparison
        Map<String, List<Map<String, Object>>> pairs = new HashMap<>();
        pairs.put("E/I", Arrays.asList(
            createScoreItem("E", scores.get("E")),
            createScoreItem("I", scores.get("I"))
        ));
        pairs.put("S/N", Arrays.asList(
            createScoreItem("S", scores.get("S")),
            createScoreItem("N", scores.get("N"))
        ));
        pairs.put("T/F", Arrays.asList(
            createScoreItem("T", scores.get("T")),
            createScoreItem("F", scores.get("F"))
        ));
        pairs.put("J/P", Arrays.asList(
            createScoreItem("J", scores.get("J")),
            createScoreItem("P", scores.get("P"))
        ));
        
        graphData.put("pairs", pairs);
        graphData.put("type", "mbti");
        graphData.put("title", "MBTI Personality Dimensions");
        graphData.put("maxValue", 35);
        
        return graphData;
    }
    
    private Map<String, Object> createScoreItem(String dimension, DetailedScoringDTO.ScoreData score) {
        Map<String, Object> item = new HashMap<>();
        item.put("dimension", dimension);
        item.put("label", score.getLabel());
        item.put("raw", score.getRaw());
        item.put("percentage", score.getPercentage());
        item.put("description", score.getDescription());
        return item;
    }
    
    /**
     * Helper method to calculate percentage
     */
    private double calculatePercentage(int raw, int max) {
        if (max == 0) return 0.0;
        return Math.round((double) raw / max * 100 * 100.0) / 100.0; // Round to 2 decimal places
    }
    
    /**
     * Helper method to sum answers in a range
     */
    private int sumAnswers(Map<Integer, Integer> answers, int start, int end) {
        int sum = 0;
        for (int i = start; i <= end; i++) {
            sum += answers.getOrDefault(i, 0);
        }
        return sum;
    }
    
    /**
     * Get all personality test scores for debugging
     */
    public List<PersonalityTestScores> getAllPersonalityTestScores() {
        return scoringRepository.findAll();
    }
    
    /**
     * Build DetailedScoringDTO from PersonalityTestScores entity
     */
    private DetailedScoringDTO buildDetailedScoringDTO(PersonalityTestScores scores) {
        DetailedScoringDTO dto = new DetailedScoringDTO();
        
        // Build RIASEC scores (with percentages)
        Map<String, DetailedScoringDTO.ScoreData> riasecScores = new HashMap<>();
        riasecScores.put("R", new DetailedScoringDTO.ScoreData(scores.getRiasecRRaw(), scores.getRiasecRPercentage().doubleValue(), "Realistic", RIASEC_DESCRIPTIONS.get("R")));
        riasecScores.put("I", new DetailedScoringDTO.ScoreData(scores.getRiasecIRaw(), scores.getRiasecIPercentage().doubleValue(), "Investigative", RIASEC_DESCRIPTIONS.get("I")));
        riasecScores.put("A", new DetailedScoringDTO.ScoreData(scores.getRiasecARaw(), scores.getRiasecAPercentage().doubleValue(), "Artistic", RIASEC_DESCRIPTIONS.get("A")));
        riasecScores.put("S", new DetailedScoringDTO.ScoreData(scores.getRiasecSRaw(), scores.getRiasecSPercentage().doubleValue(), "Social", RIASEC_DESCRIPTIONS.get("S")));
        riasecScores.put("E", new DetailedScoringDTO.ScoreData(scores.getRiasecERaw(), scores.getRiasecEPercentage().doubleValue(), "Enterprising", RIASEC_DESCRIPTIONS.get("E")));
        riasecScores.put("C", new DetailedScoringDTO.ScoreData(scores.getRiasecCRaw(), scores.getRiasecCPercentage().doubleValue(), "Conventional", RIASEC_DESCRIPTIONS.get("C")));
        dto.setRiasecScores(riasecScores);
        
        // Build MBTI scores (with percentages)
        Map<String, DetailedScoringDTO.ScoreData> mbtiScores = new HashMap<>();
        mbtiScores.put("E", new DetailedScoringDTO.ScoreData(scores.getMbtiERaw(), scores.getMbtiEPercentage().doubleValue(), "Extraversion", MBTI_DESCRIPTIONS.get("E")));
        mbtiScores.put("I", new DetailedScoringDTO.ScoreData(scores.getMbtiIRaw(), scores.getMbtiIPercentage().doubleValue(), "Introversion", MBTI_DESCRIPTIONS.get("I")));
        mbtiScores.put("S", new DetailedScoringDTO.ScoreData(scores.getMbtiSRaw(), scores.getMbtiSPercentage().doubleValue(), "Sensing", MBTI_DESCRIPTIONS.get("S")));
        mbtiScores.put("N", new DetailedScoringDTO.ScoreData(scores.getMbtiNRaw(), scores.getMbtiNPercentage().doubleValue(), "Intuition", MBTI_DESCRIPTIONS.get("N")));
        mbtiScores.put("T", new DetailedScoringDTO.ScoreData(scores.getMbtiTRaw(), scores.getMbtiTPercentage().doubleValue(), "Thinking", MBTI_DESCRIPTIONS.get("T")));
        mbtiScores.put("F", new DetailedScoringDTO.ScoreData(scores.getMbtiFRaw(), scores.getMbtiFPercentage().doubleValue(), "Feeling", MBTI_DESCRIPTIONS.get("F")));
        mbtiScores.put("J", new DetailedScoringDTO.ScoreData(scores.getMbtiJRaw(), scores.getMbtiJPercentage().doubleValue(), "Judging", MBTI_DESCRIPTIONS.get("J")));
        mbtiScores.put("P", new DetailedScoringDTO.ScoreData(scores.getMbtiPRaw(), scores.getMbtiPPercentage().doubleValue(), "Perceiving", MBTI_DESCRIPTIONS.get("P")));
        dto.setMbtiScores(mbtiScores);
        
        // Set final results
        dto.setFinalRiasecCode(scores.getFinalRiasecCode());
        dto.setFinalMbtiType(scores.getFinalMbtiType());
        
        // Generate graph data
        dto.setRiasecGraphData(generateRIASECGraphData(riasecScores));
        dto.setMbtiGraphData(generateMBTIGraphData(mbtiScores));
        
        return dto;
    }
}

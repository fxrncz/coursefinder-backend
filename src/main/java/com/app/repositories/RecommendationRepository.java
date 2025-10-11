package com.app.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.app.models.TestResult;

public interface RecommendationRepository extends JpaRepository<TestResult, Long> {

    @Query(value = """
WITH candidates AS (
  SELECT c.course_id, c.name, r.relevance_score AS riasec_rel
  FROM course c
  JOIN course_riasec_relevance r
    ON r.course_id = c.course_id
   AND r.riasec_code = :riasecCode
),
attr AS (
  SELECT ca.course_id, SUM(ca.value * maw.weight) AS mbti_attr_score
  FROM course_attribute ca
  JOIN mbti_attribute_weight maw
    ON maw.mbti_code = :mbtiCode
   AND maw.attribute_id = ca.attribute_id
  GROUP BY ca.course_id
)
SELECT cand.course_id AS courseId,
       cand.name      AS name,
       ROUND(0.6 * cand.riasec_rel + 0.4 * COALESCE(attr.mbti_attr_score,0), 3) AS totalScore
FROM candidates cand
LEFT JOIN attr ON attr.course_id = cand.course_id
ORDER BY totalScore DESC, cand.name
LIMIT :limit
""", nativeQuery = true)
    List<RankedCourseProjection> findTopCourses(
      @Param("mbtiCode") String mbtiCode,
      @Param("riasecCode") String riasecCode,
      @Param("limit") int limit
    );

    @Query(value = """
WITH candidates AS (
  SELECT c.career_id, c.name, r.relevance_score AS riasec_rel
  FROM career c
  JOIN career_riasec_relevance r
    ON r.career_id = c.career_id
   AND r.riasec_code = :riasecCode
),
attr AS (
  SELECT ca.career_id, SUM(ca.value * maw.weight) AS mbti_attr_score
  FROM career_attribute ca
  JOIN mbti_attribute_weight maw
    ON maw.mbti_code = :mbtiCode
   AND maw.attribute_id = ca.attribute_id
  GROUP BY ca.career_id
)
SELECT cand.career_id AS careerId,
       cand.name      AS name,
       ROUND(0.6 * cand.riasec_rel + 0.4 * COALESCE(attr.mbti_attr_score,0), 3) AS totalScore
FROM candidates cand
LEFT JOIN attr ON attr.career_id = cand.career_id
ORDER BY totalScore DESC, cand.name
LIMIT :limit
""", nativeQuery = true)
    List<RankedCareerProjection> findTopCareers(
      @Param("mbtiCode") String mbtiCode,
      @Param("riasecCode") String riasecCode,
      @Param("limit") int limit
    );
}



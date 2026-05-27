package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.NodeContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeContentRepository extends JpaRepository<NodeContent, Long> {

    List<NodeContent> findByActOrderByFloorAscPositionAsc(Integer act);

    List<NodeContent> findByNodeType(String nodeType);
}

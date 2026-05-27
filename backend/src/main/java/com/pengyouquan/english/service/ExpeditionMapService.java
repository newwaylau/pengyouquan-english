package com.pengyouquan.english.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengyouquan.english.model.Expedition;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 杀戮尖塔风格分支地图生成算法服务。
 * <p>
 * 每层(Act)由多行(row)组成，每行有1-2个节点。
 * 岔路行有2个节点(左/右)，玩家只能选一个前进。
 * 节点间通过 connections 字段表示指向下一行节点的索引。
 * <p>
 * 该服务为纯算法逻辑，不依赖任何数据库 Repository。
 */
@Service
public class ExpeditionMapService {

    private final ObjectMapper objectMapper;

    /**
     * 每层节点序列模板（内部数组表示分支选项，多元素表示岔路）。
     * 与 ExpeditionService 中的 ACT_NODE_TEMPLATES 保持一致。
     */
    private static final Map<Integer, List<List<String>>> ACT_NODE_TEMPLATES = new LinkedHashMap<>();

    static {
        // Act 1: 6行，第2行岔路(event/combat)，第4行岔路(combat/shop)
        ACT_NODE_TEMPLATES.put(1, List.of(
                List.of("combat"),
                List.of("event", "combat"),   // 岔路
                List.of("rest"),
                List.of("combat", "shop"),    // 岔路
                List.of("combat"),
                List.of("boss")
        ));
        // Act 2: 7行
        ACT_NODE_TEMPLATES.put(2, List.of(
                List.of("combat"),
                List.of("event", "combat"),
                List.of("rest"),
                List.of("shop", "event"),
                List.of("combat"),
                List.of("combat"),
                List.of("boss")
        ));
        // Act 3: 7行
        ACT_NODE_TEMPLATES.put(3, List.of(
                List.of("combat"),
                List.of("event", "combat"),
                List.of("rest"),
                List.of("shop", "event"),
                List.of("combat"),
                List.of("event", "combat"),
                List.of("boss")
        ));
    }

    public ExpeditionMapService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ==================== 公开 API ====================

    /**
     * 为指定层(Act)生成完整地图结构（含节点坐标与连接关系）。
     *
     * @param act 层号 (1, 2, 3)
     * @return 包含 acts 数组的 JSON 兼容 Map，形如：
     * {"acts":[{"act":1,"rows":[...]}]}
     */
    public Map<String, Object> generateMap(int act) {
        List<List<String>> template = ACT_NODE_TEMPLATES.get(act);
        if (template == null) {
            throw new IllegalArgumentException("无效的 Act: " + act + "，仅支持 1-3");
        }

        List<Map<String, Object>> rows = new ArrayList<>();

        for (int rowIdx = 0; rowIdx < template.size(); rowIdx++) {
            List<String> nodeTypes = template.get(rowIdx);
            List<Map<String, Object>> nodes = new ArrayList<>();
            boolean isBranch = nodeTypes.size() > 1;

            for (int colIdx = 0; colIdx < nodeTypes.size(); colIdx++) {
                String type = nodeTypes.get(colIdx);
                String id = (rowIdx + 1) + "-" + (colIdx + 1);

                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", id);
                node.put("type", type);
                node.put("row", rowIdx + 1);
                node.put("col", colIdx + 1);
                node.put("cleared", false);

                // 岔路节点记录分支位置
                if (isBranch) {
                    node.put("branchChoice", colIdx);
                }

                // 计算 connections：指向下一行所有节点（当前节点的可前进目标）
                List<Integer> connections = new ArrayList<>();
                if (rowIdx + 1 < template.size()) {
                    List<String> nextRowTypes = template.get(rowIdx + 1);
                    // 杀戮尖塔风格：当前行所有节点都连接到下一行所有节点
                    for (int nextCol = 0; nextCol < nextRowTypes.size(); nextCol++) {
                        connections.add(nextCol + 1); // 1-based column index
                    }
                }
                node.put("connections", connections);

                nodes.add(node);
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("nodes", nodes);
            rows.add(row);
        }

        Map<String, Object> actData = new LinkedHashMap<>();
        actData.put("act", act);
        actData.put("rows", rows);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("acts", List.of(actData));

        return result;
    }

    /**
     * 根据当前节点 ID 返回可前往的下一行节点列表。
     *
     * @param mapData      generateMap() 返回的地图数据
     * @param currentNodeId 当前节点 ID (如 "3-1")
     * @return 可前往的节点列表（空列表表示已是最后一行）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getNextNodes(Map<String, Object> mapData, String currentNodeId) {
        List<Map<String, Object>> acts = (List<Map<String, Object>>) mapData.get("acts");
        if (acts == null || acts.isEmpty()) {
            return List.of();
        }

        Map<String, Object> actData = acts.get(0);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) actData.get("rows");
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        // 找到当前节点所在行索引
        int currentRowIdx = -1;
        int currentColIdx = -1;
        for (int r = 0; r < rows.size(); r++) {
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) rows.get(r).get("nodes");
            for (int c = 0; c < nodes.size(); c++) {
                if (currentNodeId.equals(nodes.get(c).get("id"))) {
                    currentRowIdx = r;
                    currentColIdx = c;
                    break;
                }
            }
            if (currentRowIdx >= 0) break;
        }

        if (currentRowIdx < 0) {
            return List.of();
        }

        // 如果是最后一行，无后续节点
        if (currentRowIdx + 1 >= rows.size()) {
            return List.of();
        }

        // 返回下一行所有节点
        List<Map<String, Object>> nextRow = (List<Map<String, Object>>) rows.get(currentRowIdx + 1).get("nodes");
        return nextRow.stream()
                .map(n -> {
                    Map<String, Object> copy = new LinkedHashMap<>(n);
                    return copy;
                })
                .collect(Collectors.toList());
    }

    /**
     * 将远征推进到下一个节点（按行顺序自动前进）。
     * 如果是岔路行且尚未选择，不推进。
     * 如果当前行是最后一行且已完成，推进到下一层(Act)。
     *
     * @param expedition 远征实体
     * @return 推进后的结果信息
     */
    public Map<String, Object> advanceNode(Expedition expedition) {
        Map<String, Object> result = new LinkedHashMap<>();

        int currentAct = expedition.getAct();
        int currentNode = expedition.getNode();

        List<List<String>> template = ACT_NODE_TEMPLATES.get(currentAct);
        if (template == null) {
            result.put("success", false);
            result.put("message", "无效的 Act: " + currentAct);
            return result;
        }

        int currentRowIdx = currentNode - 1;

        // 检查是否已到达最后一行的下一格（跨层）
        if (currentRowIdx >= template.size()) {
            // 检查是否有下一层
            int nextAct = currentAct + 1;
            if (ACT_NODE_TEMPLATES.containsKey(nextAct)) {
                result.put("success", true);
                result.put("newAct", nextAct);
                result.put("newNode", 1);
                result.put("actCompleted", true);
                result.put("message", "进入第 " + nextAct + " 层");
            } else {
                result.put("success", true);
                result.put("expeditionComplete", true);
                result.put("message", "远征完成！");
            }
            return result;
        }

        List<String> currentRowTypes = template.get(currentRowIdx);

        // 如果是岔路行且尚未选择分支，不允许前进
        if (currentRowTypes.size() > 1) {
            int choice = getBranchChoice(expedition, currentRowIdx);
            if (choice < 0) {
                result.put("success", false);
                result.put("message", "当前为岔路节点，请先选择分支");
                result.put("isBranch", true);
                result.put("options", currentRowTypes);
                return result;
            }
        }

        // 推进到下一行
        int nextNode = currentNode + 1;
        int nextRowIdx = nextNode - 1;

        if (nextRowIdx < template.size()) {
            List<String> nextRowTypes = template.get(nextRowIdx);
            String nextType = nextRowTypes.size() > 1 ? "branch" : nextRowTypes.get(0);

            result.put("success", true);
            result.put("newNode", nextNode);
            result.put("newNodeType", nextType);

            if (nextRowTypes.size() > 1) {
                result.put("isBranch", true);
                result.put("options", nextRowTypes);
            }

            result.put("message", "前进到第 " + nextNode + " 个节点");
        } else {
            // 已到达本层末尾，检查是否有下一层
            int nextAct = currentAct + 1;
            if (ACT_NODE_TEMPLATES.containsKey(nextAct)) {
                result.put("success", true);
                result.put("newAct", nextAct);
                result.put("newNode", 1);
                result.put("actCompleted", true);
                result.put("message", "进入第 " + nextAct + " 层");
            } else {
                result.put("success", true);
                result.put("expeditionComplete", true);
                result.put("message", "远征完成！");
            }
        }

        return result;
    }

    /**
     * 记录分支选择到远征的 battleState。
     *
     * @param expedition    远征实体
     * @param choiceIndex 选择的分支索引 (0=左, 1=右)
     */
    public void chooseBranch(Expedition expedition, int choiceIndex) {
        int currentRowIdx = expedition.getNode() - 1;
        List<List<String>> template = ACT_NODE_TEMPLATES.get(expedition.getAct());
        if (template == null || currentRowIdx < 0 || currentRowIdx >= template.size()) {
            throw new IllegalArgumentException("无效的节点索引");
        }
        List<String> rowTypes = template.get(currentRowIdx);
        if (choiceIndex < 0 || choiceIndex >= rowTypes.size()) {
            throw new IllegalArgumentException("无效的分支选择: " + choiceIndex + "，可选范围 0-" + (rowTypes.size() - 1));
        }

        setBranchChoice(expedition, currentRowIdx, choiceIndex);
    }

    /**
     * 将地图数据序列化为 JSON 字符串。
     *
     * @param mapData 地图数据 Map
     * @return JSON 字符串
     */
    public String toJson(Map<String, Object> mapData) {
        try {
            return objectMapper.writeValueAsString(mapData);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 将 JSON 字符串反序列化为地图数据 Map。
     *
     * @param json JSON 字符串
     * @return 地图数据 Map
     */
    public Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 获取当前节点类型字符串（考虑分支选择）。
     *
     * @param expedition 远征实体
     * @return 节点类型 (combat/rest/shop/event/boss/branch)
     */
    public String getNodeType(Expedition expedition) {
        int act = expedition.getAct();
        int node = expedition.getNode();

        List<List<String>> template = ACT_NODE_TEMPLATES.get(act);
        if (template == null) return "combat";

        int rowIdx = node - 1;
        if (rowIdx < 0 || rowIdx >= template.size()) return "boss";

        List<String> rowTypes = template.get(rowIdx);
        if (rowTypes.isEmpty()) return "combat";

        if (rowTypes.size() == 1) {
            return rowTypes.get(0);
        }

        // 岔路：检查是否已选择
        int choice = getBranchChoice(expedition, rowIdx);
        if (choice < 0 || choice >= rowTypes.size()) {
            return "branch";
        }
        return rowTypes.get(choice);
    }

    /**
     * 获取当前行的所有可选节点类型（用于岔路展示）。
     *
     * @param expedition 远征实体
     * @return 选项列表
     */
    public List<String> getNodeOptions(Expedition expedition) {
        int act = expedition.getAct();
        int node = expedition.getNode();

        List<List<String>> template = ACT_NODE_TEMPLATES.get(act);
        if (template == null) return List.of();

        int rowIdx = node - 1;
        if (rowIdx < 0 || rowIdx >= template.size()) return List.of();

        return template.get(rowIdx);
    }

    /**
     * 获取当前节点在模板中的索引。
     *
     * @param expedition 远征实体
     * @return 当前行索引 (0-based)
     */
    public int getCurrentRowIndex(Expedition expedition) {
        return expedition.getNode() - 1;
    }

    /**
     * 判断当前是否为岔路节点。
     *
     * @param expedition 远征实体
     * @return true 如果是岔路
     */
    public boolean isBranchNode(Expedition expedition) {
        int act = expedition.getAct();
        int node = expedition.getNode();

        List<List<String>> template = ACT_NODE_TEMPLATES.get(act);
        if (template == null) return false;

        int rowIdx = node - 1;
        if (rowIdx < 0 || rowIdx >= template.size()) return false;

        return template.get(rowIdx).size() > 1;
    }

    /**
     * 获取指定层(Act)的模板布局。
     *
     * @param act 层号
     * @return 模板布局，key为行号(1-based)，value为该行节点类型列表
     */
    public Map<Integer, List<String>> getActLayout(int act) {
        List<List<String>> template = ACT_NODE_TEMPLATES.get(act);
        if (template == null) {
            return new LinkedHashMap<>();
        }
        Map<Integer, List<String>> layout = new LinkedHashMap<>();
        for (int i = 0; i < template.size(); i++) {
            layout.put(i + 1, template.get(i));
        }
        return layout;
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 从 battle_state 中读取指定行的分支选择。
     */
    private int getBranchChoice(Expedition expedition, int rowIndex) {
        try {
            String bs = expedition.getBattleState();
            if (bs == null || bs.isBlank() || "{}".equals(bs)) return -1;
            Map<String, Object> state = objectMapper.readValue(bs, Map.class);
            Object choices = state.get("nodeChoices");
            if (choices instanceof Map) {
                Number choice = (Number) ((Map<?, ?>) choices).get(String.valueOf(rowIndex));
                return choice != null ? choice.intValue() : -1;
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    /**
     * 记录分支选择到 battle_state。
     */
    private void setBranchChoice(Expedition expedition, int rowIndex, int choice) {
        try {
            String bs = expedition.getBattleState();
            Map<String, Object> state;
            if (bs == null || bs.isBlank() || "{}".equals(bs)) {
                state = new HashMap<>();
            } else {
                state = objectMapper.readValue(bs, Map.class);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> choices = (Map<String, Object>) state.computeIfAbsent("nodeChoices",
                    k -> new HashMap<String, Object>());
            choices.put(String.valueOf(rowIndex), choice);
            expedition.setBattleState(objectMapper.writeValueAsString(state));
        } catch (Exception ignored) {
        }
    }

    /**
     * 获取所有支持的 Act 号。
     *
     * @return Act 号列表
     */
    public Set<Integer> getAvailableActs() {
        return ACT_NODE_TEMPLATES.keySet();
    }
}

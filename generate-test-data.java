import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 电力交易测试数据生成器（独立版本）
 * 生成 20 份电力交易规则和政策文件
 */
public class TestDataGenerator {

    private static final String OUTPUT_DIR = "data/test-documents";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        System.out.println("===========================================" );
        System.out.println("电力交易测试数据生成器");
        System.out.println("===========================================" );
        System.out.println();
        
        try {
            // 创建输出目录
            File outputDir = new File(OUTPUT_DIR);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
                System.out.println("[√] 创建目录：" + new File(OUTPUT_DIR).getAbsolutePath());
            }

            // 生成 20 份文档
            List<DocumentData> documents = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                DocumentData doc = generateDocument(i + 1);
                documents.add(doc);
                
                // 保存为 TXT 文件
                saveDocument(doc);
                System.out.println("[√] 生成文档 [" + String.format("%02d", i + 1) + "/20]: " + doc.fileName);
            }

            // 生成 SQL 导入脚本
            generateSQLScript(documents);
            System.out.println("[√] 生成 SQL 导入脚本完成");

            // 生成 JSON 元数据
            generateMetadataJSON(documents);
            System.out.println("[√] 生成元数据文件完成");

            System.out.println();
            System.out.println("===========================================" );
            System.out.println("测试数据生成完成！");
            System.out.println("===========================================" );
            System.out.println("文档目录：" + new File(OUTPUT_DIR).getAbsolutePath());
            System.out.println("文档数量：20 份");
            System.out.println("知识库：KB001（电力交易政策法规库）");
            System.out.println();

        } catch (Exception e) {
            System.err.println("生成测试数据失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static DocumentData generateDocument(int index) {
        DocumentData doc = new DocumentData();
        
        doc.docId = "DOC" + String.format("%03d", index);
        doc.kbId = "KB001";
        
        String title = DOCUMENT_TITLES[index - 1];
        String agency = AGENCIES[index % AGENCIES.length];
        
        doc.title = agency + "关于印发《" + title + "》的通知";
        doc.fileName = "DOC" + String.format("%03d", index) + "_" + getShortTitle(title) + ".txt";
        doc.docNumber = agency.substring(0, 3) + "发〔202" + (index % 5) + "〕" + String.format("%03d", index) + "号";
        
        LocalDate publishDate = LocalDate.of(2020 + (index % 5), 1 + (index % 12), 1 + (index % 28));
        doc.publishDate = publishDate.format(DATE_FORMAT);
        
        LocalDate implementDate = publishDate.plusMonths(1 + (index % 3));
        doc.implementDate = implementDate.format(DATE_FORMAT);
        
        doc.docType = "POLICY";
        doc.content = generateDocumentContent(doc);
        doc.fileSize = doc.content.length();
        doc.tags = generateTags(index);
        doc.summary = generateSummary(doc);
        
        return doc;
    }

    private static String generateDocumentContent(DocumentData doc) {
        StringBuilder content = new StringBuilder();
        
        content.append(doc.title).append("\n\n");
        content.append(doc.docNumber).append("\n\n");
        content.append("========================================\n");
        content.append(doc.publishDate).append("\n");
        content.append("========================================\n\n");
        
        for (int chapter = 1; chapter <= 10; chapter++) {
            content.append("第").append(toChineseNumber(chapter)).append("章 ")
                   .append(CHAPTER_TITLES[chapter - 1]).append("\n\n");
            
            int articles = 3 + (chapter % 3);
            for (int article = 1; article <= articles; article++) {
                content.append("第").append(toChineseNumber((chapter - 1) * 5 + article)).append("条 ");
                content.append(generateArticleContent(chapter, article, doc)).append("\n\n");
            }
        }
        
        content.append("\n========================================\n");
        content.append("本").append(doc.docType).append("自").append(doc.implementDate)
               .append("起施行，有效期").append(3 + (doc.docId.hashCode() % 5)).append("年。\n");
        content.append("========================================\n");
        
        return content.toString();
    }

    private static String generateArticleContent(int chapter, int article, DocumentData doc) {
        Random random = new Random(doc.docId.hashCode() + chapter * 10 + article);
        
        String[] templates = {
            "电力交易机构应当按照公平、公正、公开的原则，组织市场成员开展交易活动。",
            "市场成员应当遵守电力市场交易规则，履行相关信息披露义务。",
            "发电企业、电力用户、售电公司应当按照规定参与电力市场交易。",
            "电力调度机构应当保障电网安全稳定运行，维护各方合法权益。",
            "交易价格应当反映电力供需关系，促进资源优化配置。",
            "市场成员应当建立健全风险管理制度，防范市场风险。",
            "电力交易机构应当及时发布市场信息，接受社会监督。",
            "违反本规定的，由能源主管部门责令改正，并依法予以处罚。",
            "鼓励可再生能源发电企业参与电力市场交易。",
            "电力市场交易应当采用电子化方式，提高交易效率。",
            "市场主体应当诚实守信，维护良好的市场秩序。",
            "建立电力市场信用评价体系，实施信用分类监管。",
            "电力交易合同应当明确约定交易电量、电价、结算方式等内容。",
            "跨省跨区交易应当符合国家能源战略和电力发展规划。",
            "电力辅助服务补偿费用纳入输配电价回收。"
        };
        
        return templates[random.nextInt(templates.length)];
    }

    private static List<String> generateTags(int index) {
        List<String> tags = new ArrayList<>();
        tags.add("电力交易");
        tags.add("市场规则");
        
        if (index <= 5) tags.add("中长期交易");
        else if (index <= 10) tags.add("现货市场");
        else if (index <= 15) tags.add("辅助服务");
        else tags.add("可再生能源");
        
        tags.add("政策法规");
        
        return tags;
    }

    private static String generateSummary(DocumentData doc) {
        return "本文档规定了" + doc.title + "，明确了市场成员的权利义务、交易方式、价格机制、结算管理等内容，" +
               "自" + doc.implementDate + "起施行，有效期" + (3 + (doc.docId.hashCode() % 5)) + "年。";
    }

    private static void saveDocument(DocumentData doc) throws IOException {
        String filePath = OUTPUT_DIR + "/" + doc.fileName;
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(doc.content);
        }
    }

    private static void generateSQLScript(List<DocumentData> documents) throws IOException {
        StringBuilder sql = new StringBuilder();
        
        sql.append("-- ==========================================\n");
        sql.append("-- 电力交易测试数据导入脚本\n");
        sql.append("-- 生成时间：").append(LocalDate.now()).append("\n");
        sql.append("-- 文档数量：20 份\n");
        sql.append("-- ==========================================\n\n");
        sql.append("USE power_trade_rag;\n\n");
        sql.append("-- 插入文档元数据\n");
        
        for (DocumentData doc : documents) {
            sql.append("INSERT INTO document_info (doc_id, kb_id, title, file_name, file_size, doc_type, status, create_time) ");
            sql.append("VALUES ('").append(doc.docId).append("', '");
            sql.append(doc.kbId).append("', '");
            sql.append(escapeSql(doc.title)).append("', '");
            sql.append(escapeSql(doc.fileName)).append("', ");
            sql.append(doc.fileSize).append(", '");
            sql.append(doc.docType).append("', 1, NOW());\n");
        }
        
        sql.append("\n-- 插入知识库信息\n");
        sql.append("INSERT INTO knowledge_base (kb_id, name, description, status, create_time) ");
        sql.append("VALUES ('KB001', '电力交易政策法规库', '收录国家及地方电力交易相关政策法规文件', 1, NOW());\n");
        sql.append("INSERT INTO knowledge_base (kb_id, name, description, status, create_time) ");
        sql.append("VALUES ('KB002', '电力市场交易规则库', '收录电力市场各类交易规则和实施细则', 1, NOW());\n");
        sql.append("INSERT INTO knowledge_base (kb_id, name, description, status, create_time) ");
        sql.append("VALUES ('KB003', '电力辅助服务管理办法库', '收录电力辅助服务相关管理办法和规定', 1, NOW());\n");
        
        String filePath = OUTPUT_DIR + "/import_test_data.sql";
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(sql.toString());
        }
    }

    private static void generateMetadataJSON(List<DocumentData> documents) throws IOException {
        StringBuilder json = new StringBuilder();
        
        json.append("{\n");
        json.append("  \"generated_date\": \"").append(LocalDate.now()).append("\",\n");
        json.append("  \"total_documents\": ").append(documents.size()).append(",\n");
        json.append("  \"description\": \"电力交易规则和政策测试数据集\",\n");
        json.append("  \"documents\": [\n");
        
        for (int i = 0; i < documents.size(); i++) {
            DocumentData doc = documents.get(i);
            json.append("    {\n");
            json.append("      \"doc_id\": \"").append(doc.docId).append("\",\n");
            json.append("      \"kb_id\": \"").append(doc.kbId).append("\",\n");
            json.append("      \"title\": \"").append(escapeJson(doc.title)).append("\",\n");
            json.append("      \"file_name\": \"").append(escapeJson(doc.fileName)).append("\",\n");
            json.append("      \"doc_number\": \"").append(escapeJson(doc.docNumber)).append("\",\n");
            json.append("      \"publish_date\": \"").append(doc.publishDate).append("\",\n");
            json.append("      \"implement_date\": \"").append(doc.implementDate).append("\",\n");
            json.append("      \"doc_type\": \"").append(doc.docType).append("\",\n");
            json.append("      \"file_size\": ").append(doc.fileSize).append(",\n");
            json.append("      \"tags\": ").append(doc.tags.toString()).append(",\n");
            json.append("      \"summary\": \"").append(escapeJson(doc.summary)).append("\"\n");
            json.append("    }");
            if (i < documents.size() - 1) json.append(",");
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}\n");
        
        String filePath = OUTPUT_DIR + "/documents_metadata.json";
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(json.toString());
        }
    }

    private static String toChineseNumber(int num) {
        String[] chinese = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
        return (num >= 1 && num <= 10) ? chinese[num - 1] : String.valueOf(num);
    }

    private static String getShortTitle(String title) {
        return title.replaceAll("[\\[\\]《》\\s]", "");
    }

    private static String escapeSql(String str) {
        return str == null ? "" : str.replace("'", "''");
    }

    private static String escapeJson(String str) {
        return str == null ? "" : str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    // 文档标题数组
    private static final String[] DOCUMENT_TITLES = {
        "电力中长期交易基本规则",
        "电力现货市场交易规则",
        "电力辅助服务管理办法",
        "可再生能源电力消纳保障机制",
        "电力市场信息披露管理办法",
        "电力零售市场交易规则",
        "电力批发市场监管办法",
        "跨省跨区电力交易规则",
        "电力市场结算管理办法",
        "发电企业并网运行管理规定",
        "电力用户参与市场交易实施细则",
        "售电公司管理办法",
        "电力市场风险防控指引",
        "电力交易机构组建和规范运行办法",
        "电力市场秩序监管暂行办法",
        "新能源参与电力市场交易指导意见",
        "电力需求侧管理办法",
        "电力市场信用体系建设指导意见",
        "电力市场运营规则",
        "电力交易合同示范文本"
    };

    private static final String[] AGENCIES = {
        "国家能源局",
        "国家发展和改革委员会",
        "工业和信息化部",
        "各省能源局",
        "电力交易中心",
        "电网公司"
    };

    private static final String[] CHAPTER_TITLES = {
        "总则",
        "市场成员",
        "交易品种",
        "交易方式",
        "价格机制",
        "计量与结算",
        "信息披露",
        "风险防控",
        "监督管理",
        "附则"
    };

    static class DocumentData {
        String docId;
        String kbId;
        String title;
        String fileName;
        String docNumber;
        String publishDate;
        String implementDate;
        String docType;
        long fileSize;
        String content;
        List<String> tags;
        String summary;
    }
}

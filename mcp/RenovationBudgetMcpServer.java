import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Standalone MCP server for renovation budget estimation.
 *
 * This file intentionally has no dependency on the Spring project or any
 * third-party MCP/JSON library. It implements a small JSON-RPC stdio MCP
 * surface with one tool: calculate_renovation_budget.
 *
 * Compile:
 *   javac mcp/RenovationBudgetMcpServer.java
 *
 * Run as an MCP stdio server:
 *   java -cp mcp RenovationBudgetMcpServer
 */
public final class RenovationBudgetMcpServer {
    private static final Map<String, Double> BASE_UNIT_PRICE = Map.of(
            "basic", 900.0,
            "standard", 1500.0,
            "premium", 2400.0,
            "luxury", 3800.0
    );

    private static final Map<String, Double> CITY_MULTIPLIER = Map.of(
            "tier1", 1.18,
            "new_tier1", 1.08,
            "tier2", 1.0,
            "tier3_or_lower", 0.9
    );

    private static final Map<String, Double> RENOVATION_MULTIPLIER = Map.of(
            "new_home", 1.0,
            "old_home_partial", 1.12,
            "old_home_full", 1.28
    );

    private static final Map<String, Object> SERVER_INFO = object(
            "name", "renovation-budget-mcp-server",
            "version", "1.0.0"
    );

    private static final Map<String, Object> TOOL = object(
            "name", "calculate_renovation_budget",
            "description", "Estimate a home renovation budget with itemized ranges, contingency, and risk notes.",
            "inputSchema", object(
                    "type", "object",
                    "additionalProperties", false,
                    "properties", object(
                            "areaSqm", object(
                                    "type", "number",
                                    "minimum", 10,
                                    "description", "Home area in square meters."
                            ),
                            "cityTier", object(
                                    "type", "string",
                                    "enum", list("tier1", "new_tier1", "tier2", "tier3_or_lower"),
                                    "description", "City cost level."
                            ),
                            "finishLevel", object(
                                    "type", "string",
                                    "enum", list("basic", "standard", "premium", "luxury"),
                                    "description", "Overall decoration level."
                            ),
                            "renovationType", object(
                                    "type", "string",
                                    "enum", list("new_home", "old_home_partial", "old_home_full"),
                                    "description", "New home or old-home renovation type."
                            ),
                            "rooms", object(
                                    "type", "integer",
                                    "minimum", 1,
                                    "description", "Number of bedrooms."
                            ),
                            "bathrooms", object(
                                    "type", "integer",
                                    "minimum", 1,
                                    "description", "Number of bathrooms."
                            ),
                            "kitchenCount", object(
                                    "type", "integer",
                                    "minimum", 1,
                                    "description", "Number of kitchens."
                            ),
                            "hasCustomCabinets", object(
                                    "type", "boolean",
                                    "description", "Whether the plan includes significant custom cabinets."
                            ),
                            "hasFloorHeating", object(
                                    "type", "boolean",
                                    "description", "Whether the plan includes floor heating."
                            ),
                            "hasCentralAc", object(
                                    "type", "boolean",
                                    "description", "Whether the plan includes central air conditioning."
                            ),
                            "hasSmartHome", object(
                                    "type", "boolean",
                                    "description", "Whether the plan includes smart-home systems."
                            ),
                            "designFeeIncluded", object(
                                    "type", "boolean",
                                    "description", "Whether to include design fees."
                            ),
                            "contingencyPercent", object(
                                    "type", "number",
                                    "minimum", 0,
                                    "maximum", 30,
                                    "description", "Contingency percentage. Default is 10."
                            ),
                            "currency", object(
                                    "type", "string",
                                    "enum", list("CNY"),
                                    "description", "Currency. Currently only CNY is supported."
                            )
                    ),
                    "required", list("areaSqm")
            )
    );

    private RenovationBudgetMcpServer() {
    }

    public static void main(String[] args) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    Object parsed = Json.parse(line);
                    if (!(parsed instanceof Map<?, ?> message)) {
                        failure(null, -32600, "Invalid request");
                        continue;
                    }
                    handleRequest(castMap(message));
                } catch (JsonException exception) {
                    failure(null, -32700, "Parse error");
                }
            }
        }
    }

    private static void handleRequest(Map<String, Object> message) {
        Object id = message.get("id");
        String method = stringOrNull(message.get("method"));

        try {
            if ("initialize".equals(method)) {
                Map<String, Object> params = mapOrEmpty(message.get("params"));
                String protocolVersion = stringOr(params.get("protocolVersion"), "2024-11-05");
                success(id, object(
                        "protocolVersion", protocolVersion,
                        "capabilities", object("tools", object()),
                        "serverInfo", SERVER_INFO
                ));
                return;
            }

            if ("notifications/initialized".equals(method)) {
                return;
            }

            if ("tools/list".equals(method)) {
                success(id, object("tools", list(TOOL)));
                return;
            }

            if ("tools/call".equals(method)) {
                Map<String, Object> params = mapOrEmpty(message.get("params"));
                String name = stringOrNull(params.get("name"));
                if (!"calculate_renovation_budget".equals(name)) {
                    failure(id, -32602, "Unknown tool: " + name);
                    return;
                }

                Map<String, Object> arguments = mapOrEmpty(params.get("arguments"));
                Map<String, Object> result = calculateBudget(arguments);
                success(id, object(
                        "content", list(object(
                                "type", "text",
                                "text", formatBudgetReport(result)
                        )),
                        "structuredContent", result
                ));
                return;
            }

            failure(id, -32601, "Method not found: " + method);
        } catch (RuntimeException exception) {
            failure(id, -32000, exception.getMessage());
        }
    }

    private static Map<String, Object> calculateBudget(Map<String, Object> input) {
        BudgetArgs args = BudgetArgs.from(input);
        validateArgs(args);

        double baseUnitPrice = BASE_UNIT_PRICE.get(args.finishLevel);
        double cityMultiplier = CITY_MULTIPLIER.get(args.cityTier);
        double renovationMultiplier = RENOVATION_MULTIPLIER.get(args.renovationType);
        double adjustedUnitPrice = baseUnitPrice * cityMultiplier * renovationMultiplier;
        double baseConstruction = args.areaSqm * adjustedUnitPrice;

        double kitchenBathroomPremium = args.kitchenCount * 18000.0 + args.bathrooms * 12000.0;
        double roomComplexity = Math.max(0, args.rooms - 2) * 6000.0;

        double hardDecoration = baseConstruction * 0.42 + kitchenBathroomPremium + roomComplexity;
        double mainMaterials = baseConstruction * 0.28;
        double furnitureAndSoftDecor = baseConstruction * 0.18;
        double appliances = baseConstruction * 0.08 + args.bathrooms * 2500.0 + args.kitchenCount * 6000.0;

        List<BudgetItem> optionalItems = new ArrayList<>();
        if (args.hasCustomCabinets) {
            optionalItems.add(item("customCabinets", "Custom cabinets", args.areaSqm * 450.0, args.areaSqm * 900.0));
        }
        if (args.hasFloorHeating) {
            optionalItems.add(item("floorHeating", "Floor heating", args.areaSqm * 180.0, args.areaSqm * 320.0));
        }
        if (args.hasCentralAc) {
            optionalItems.add(item("centralAc", "Central air conditioning", args.areaSqm * 260.0, args.areaSqm * 480.0));
        }
        if (args.hasSmartHome) {
            optionalItems.add(item("smartHome", "Smart-home system", 15000.0, Math.max(28000.0, args.areaSqm * 350.0)));
        }

        BudgetItem designFee = args.designFeeIncluded
                ? item("designFee", "Design fee", args.areaSqm * 80.0, args.areaSqm * 220.0)
                : item("designFee", "Design fee", 0, 0);

        List<BudgetItem> baseItems = new ArrayList<>();
        baseItems.add(item("hardDecoration", "Hard decoration and labor", hardDecoration * 0.9, hardDecoration * 1.15));
        baseItems.add(item("mainMaterials", "Main materials", mainMaterials * 0.9, mainMaterials * 1.2));
        baseItems.add(item("furnitureAndSoftDecor", "Furniture and soft decor", furnitureAndSoftDecor * 0.8, furnitureAndSoftDecor * 1.35));
        baseItems.add(item("appliances", "Appliances and equipment", appliances * 0.85, appliances * 1.25));
        baseItems.add(designFee);
        baseItems.addAll(optionalItems);

        double subtotalLow = baseItems.stream().mapToDouble(BudgetItem::low).sum();
        double subtotalHigh = baseItems.stream().mapToDouble(BudgetItem::high).sum();
        double contingencyLow = subtotalLow * args.contingencyPercent / 100.0;
        double contingencyHigh = subtotalHigh * args.contingencyPercent / 100.0;
        double totalLow = subtotalLow + contingencyLow;
        double totalHigh = subtotalHigh + contingencyHigh;

        List<Map<String, Object>> resultItems = baseItems.stream()
                .map(entry -> object(
                        "key", entry.key(),
                        "name", entry.name(),
                        "low", roundToHundred(entry.low()),
                        "high", roundToHundred(entry.high()),
                        "shareHint", percentage(midpoint(entry.low(), entry.high()), midpoint(totalLow, totalHigh))
                ))
                .toList();

        return object(
                "currency", args.currency,
                "assumptions", object(
                        "areaSqm", args.areaSqm,
                        "cityTier", args.cityTier,
                        "finishLevel", args.finishLevel,
                        "renovationType", args.renovationType,
                        "adjustedUnitPrice", round(adjustedUnitPrice),
                        "contingencyPercent", args.contingencyPercent
                ),
                "totalRange", object(
                        "low", roundToHundred(totalLow),
                        "high", roundToHundred(totalHigh)
                ),
                "subtotalRange", object(
                        "low", roundToHundred(subtotalLow),
                        "high", roundToHundred(subtotalHigh)
                ),
                "contingencyRange", object(
                        "low", roundToHundred(contingencyLow),
                        "high", roundToHundred(contingencyHigh)
                ),
                "items", resultItems,
                "riskNotes", buildRiskNotes(args),
                "savingTips", buildSavingTips(args),
                "disclaimer", "This is a planning estimate, not a formal quotation. Confirm final pricing with measured drawings, material brands, quantities, and local labor rates."
        );
    }

    private static void validateArgs(BudgetArgs args) {
        if (!Double.isFinite(args.areaSqm) || args.areaSqm < 10.0) {
            throw new IllegalArgumentException("areaSqm must be a number greater than or equal to 10.");
        }
        if (!BASE_UNIT_PRICE.containsKey(args.finishLevel)) {
            throw new IllegalArgumentException("finishLevel must be one of: basic, standard, premium, luxury.");
        }
        if (!CITY_MULTIPLIER.containsKey(args.cityTier)) {
            throw new IllegalArgumentException("cityTier must be one of: tier1, new_tier1, tier2, tier3_or_lower.");
        }
        if (!RENOVATION_MULTIPLIER.containsKey(args.renovationType)) {
            throw new IllegalArgumentException("renovationType must be one of: new_home, old_home_partial, old_home_full.");
        }
        if (!"CNY".equals(args.currency)) {
            throw new IllegalArgumentException("Only CNY is supported.");
        }
    }

    private static List<String> buildRiskNotes(BudgetArgs args) {
        List<String> notes = new ArrayList<>();
        if (!"new_home".equals(args.renovationType)) {
            notes.add("Old-home renovation can expose hidden costs in demolition, waterproofing, wall leveling, and water/electrical rerouting.");
        }
        if (args.bathrooms >= 2) {
            notes.add("Multiple bathrooms increase waterproofing, tiling, fixtures, plumbing, and ventilation costs.");
        }
        if (args.hasCustomCabinets) {
            notes.add("Custom cabinets are sensitive to board grade, hardware brand, door finish, and installation complexity.");
        }
        if ("premium".equals(args.finishLevel) || "luxury".equals(args.finishLevel)) {
            notes.add("Premium finishes have wider quotation variance; lock material brands and model numbers early.");
        }
        if (notes.isEmpty()) {
            notes.add("Main cost variance is likely to come from material brand choices, labor scope, and site-specific measurements.");
        }
        return notes;
    }

    private static List<String> buildSavingTips(BudgetArgs args) {
        List<String> tips = new ArrayList<>();
        tips.add("Separate must-have safety and functional items from style upgrades before requesting quotes.");
        tips.add("Ask contractors to quote labor, main materials, cabinets, appliances, and optional systems separately.");
        tips.add("Reserve contingency before choosing decorative upgrades.");
        if (args.hasCustomCabinets) {
            tips.add("Keep high-value custom cabinets in entry, kitchen, and bedroom; use finished furniture for low-frequency storage.");
        }
        if (args.hasCentralAc || args.hasFloorHeating) {
            tips.add("Confirm equipment capacity, pipe routing, maintenance access, and ceiling/floor height impact before signing.");
        }
        return tips;
    }

    private static String formatBudgetReport(Map<String, Object> result) {
        Map<String, Object> totalRange = mapOrEmpty(result.get("totalRange"));
        Map<String, Object> subtotalRange = mapOrEmpty(result.get("subtotalRange"));
        Map<String, Object> contingencyRange = mapOrEmpty(result.get("contingencyRange"));
        String currency = stringOr(result.get("currency"), "CNY");
        StringBuilder report = new StringBuilder();
        report.append("Estimated total: ")
                .append(formatMoney(totalRange.get("low")))
                .append(" - ")
                .append(formatMoney(totalRange.get("high")))
                .append(' ')
                .append(currency)
                .append('\n');
        report.append("Subtotal: ")
                .append(formatMoney(subtotalRange.get("low")))
                .append(" - ")
                .append(formatMoney(subtotalRange.get("high")))
                .append(' ')
                .append(currency)
                .append('\n');
        report.append("Contingency: ")
                .append(formatMoney(contingencyRange.get("low")))
                .append(" - ")
                .append(formatMoney(contingencyRange.get("high")))
                .append(' ')
                .append(currency)
                .append("\n\n");

        report.append("Itemized range:\n");
        for (Object value : listOrEmpty(result.get("items"))) {
            Map<String, Object> entry = mapOrEmpty(value);
            report.append("- ")
                    .append(entry.get("name"))
                    .append(": ")
                    .append(formatMoney(entry.get("low")))
                    .append(" - ")
                    .append(formatMoney(entry.get("high")))
                    .append(' ')
                    .append(currency)
                    .append(" (")
                    .append(entry.get("shareHint"))
                    .append(")\n");
        }

        report.append("\nRisk notes:\n");
        for (Object note : listOrEmpty(result.get("riskNotes"))) {
            report.append("- ").append(note).append('\n');
        }

        report.append("\nSaving tips:\n");
        for (Object tip : listOrEmpty(result.get("savingTips"))) {
            report.append("- ").append(tip).append('\n');
        }

        report.append('\n').append(result.get("disclaimer"));
        return report.toString();
    }

    private static BudgetItem item(String key, String name, double low, double high) {
        return new BudgetItem(key, name, Math.max(0.0, low), Math.max(0.0, high));
    }

    private static double midpoint(double low, double high) {
        return (low + high) / 2.0;
    }

    private static String percentage(double value, double total) {
        if (total == 0.0) {
            return "0%";
        }
        return Math.round(value / total * 100.0) + "%";
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static long roundToHundred(double value) {
        return Math.round(value / 100.0) * 100L;
    }

    private static String formatMoney(Object value) {
        if (value instanceof Number number) {
            return String.format(Locale.CHINA, "%,d", number.longValue());
        }
        return String.valueOf(value);
    }

    private static void success(Object id, Object result) {
        writeJson(object("jsonrpc", "2.0", "id", id, "result", result));
    }

    private static void failure(Object id, int code, String message) {
        writeJson(object(
                "jsonrpc", "2.0",
                "id", id,
                "error", object("code", code, "message", message)
        ));
    }

    private static void writeJson(Object payload) {
        System.out.println(Json.stringify(payload));
        System.out.flush();
    }

    private record BudgetItem(String key, String name, double low, double high) {
    }

    private static final class BudgetArgs {
        private final double areaSqm;
        private final String cityTier;
        private final String finishLevel;
        private final String renovationType;
        private final int rooms;
        private final int bathrooms;
        private final int kitchenCount;
        private final boolean hasCustomCabinets;
        private final boolean hasFloorHeating;
        private final boolean hasCentralAc;
        private final boolean hasSmartHome;
        private final boolean designFeeIncluded;
        private final double contingencyPercent;
        private final String currency;

        private BudgetArgs(
                double areaSqm,
                String cityTier,
                String finishLevel,
                String renovationType,
                int rooms,
                int bathrooms,
                int kitchenCount,
                boolean hasCustomCabinets,
                boolean hasFloorHeating,
                boolean hasCentralAc,
                boolean hasSmartHome,
                boolean designFeeIncluded,
                double contingencyPercent,
                String currency
        ) {
            this.areaSqm = areaSqm;
            this.cityTier = cityTier;
            this.finishLevel = finishLevel;
            this.renovationType = renovationType;
            this.rooms = rooms;
            this.bathrooms = bathrooms;
            this.kitchenCount = kitchenCount;
            this.hasCustomCabinets = hasCustomCabinets;
            this.hasFloorHeating = hasFloorHeating;
            this.hasCentralAc = hasCentralAc;
            this.hasSmartHome = hasSmartHome;
            this.designFeeIncluded = designFeeIncluded;
            this.contingencyPercent = contingencyPercent;
            this.currency = currency;
        }

        private static BudgetArgs from(Map<String, Object> args) {
            return new BudgetArgs(
                    numberOr(args.get("areaSqm"), 0.0),
                    stringOr(args.get("cityTier"), "tier2"),
                    stringOr(args.get("finishLevel"), "standard"),
                    stringOr(args.get("renovationType"), "new_home"),
                    integerOr(args.get("rooms"), 2),
                    integerOr(args.get("bathrooms"), 1),
                    integerOr(args.get("kitchenCount"), 1),
                    booleanOr(args.get("hasCustomCabinets"), false),
                    booleanOr(args.get("hasFloorHeating"), false),
                    booleanOr(args.get("hasCentralAc"), false),
                    booleanOr(args.get("hasSmartHome"), false),
                    booleanOr(args.get("designFeeIncluded"), true),
                    numberOr(args.get("contingencyPercent"), 10.0),
                    stringOr(args.get("currency"), "CNY")
            );
        }
    }

    @SafeVarargs
    private static <T> List<T> list(T... values) {
        return List.of(values);
    }

    private static Map<String, Object> object(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Object arguments must be key/value pairs.");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            map.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapOrEmpty(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static List<?> listOrEmpty(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        return List.of();
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String stringOr(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private static double numberOr(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static int integerOr(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean booleanOr(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return fallback;
    }

    private static final class Json {
        private Json() {
        }

        static Object parse(String text) {
            Parser parser = new Parser(text);
            Object value = parser.parseValue();
            parser.skipWhitespace();
            if (!parser.isAtEnd()) {
                throw new JsonException("Unexpected trailing content.");
            }
            return value;
        }

        static String stringify(Object value) {
            StringBuilder builder = new StringBuilder();
            writeValue(builder, value);
            return builder.toString();
        }

        private static void writeValue(StringBuilder builder, Object value) {
            if (value == null) {
                builder.append("null");
            } else if (value instanceof String text) {
                writeString(builder, text);
            } else if (value instanceof Number number) {
                builder.append(number);
            } else if (value instanceof Boolean bool) {
                builder.append(bool);
            } else if (value instanceof Map<?, ?> map) {
                writeObject(builder, map);
            } else if (value instanceof Iterable<?> iterable) {
                writeArray(builder, iterable);
            } else {
                writeString(builder, String.valueOf(value));
            }
        }

        private static void writeObject(StringBuilder builder, Map<?, ?> map) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                writeString(builder, String.valueOf(entry.getKey()));
                builder.append(':');
                writeValue(builder, entry.getValue());
            }
            builder.append('}');
        }

        private static void writeArray(StringBuilder builder, Iterable<?> values) {
            builder.append('[');
            boolean first = true;
            for (Object value : values) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                writeValue(builder, value);
            }
            builder.append(']');
        }

        private static void writeString(StringBuilder builder, String value) {
            builder.append('"');
            for (int index = 0; index < value.length(); index++) {
                char c = value.charAt(index);
                switch (c) {
                    case '"' -> builder.append("\\\"");
                    case '\\' -> builder.append("\\\\");
                    case '\b' -> builder.append("\\b");
                    case '\f' -> builder.append("\\f");
                    case '\n' -> builder.append("\\n");
                    case '\r' -> builder.append("\\r");
                    case '\t' -> builder.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            builder.append(String.format("\\u%04x", (int) c));
                        } else {
                            builder.append(c);
                        }
                    }
                }
            }
            builder.append('"');
        }

        private static final class Parser {
            private final String text;
            private int position;

            private Parser(String text) {
                this.text = text;
            }

            private Object parseValue() {
                skipWhitespace();
                if (isAtEnd()) {
                    throw new JsonException("Unexpected end of input.");
                }
                char current = text.charAt(position);
                return switch (current) {
                    case '{' -> parseObject();
                    case '[' -> parseArray();
                    case '"' -> parseString();
                    case 't' -> parseLiteral("true", Boolean.TRUE);
                    case 'f' -> parseLiteral("false", Boolean.FALSE);
                    case 'n' -> parseLiteral("null", null);
                    default -> {
                        if (current == '-' || Character.isDigit(current)) {
                            yield parseNumber();
                        }
                        throw new JsonException("Unexpected character: " + current);
                    }
                };
            }

            private Map<String, Object> parseObject() {
                expect('{');
                Map<String, Object> object = new LinkedHashMap<>();
                skipWhitespace();
                if (consume('}')) {
                    return object;
                }
                while (true) {
                    skipWhitespace();
                    String key = parseString();
                    skipWhitespace();
                    expect(':');
                    Object value = parseValue();
                    object.put(key, value);
                    skipWhitespace();
                    if (consume('}')) {
                        return object;
                    }
                    expect(',');
                }
            }

            private List<Object> parseArray() {
                expect('[');
                List<Object> array = new ArrayList<>();
                skipWhitespace();
                if (consume(']')) {
                    return array;
                }
                while (true) {
                    array.add(parseValue());
                    skipWhitespace();
                    if (consume(']')) {
                        return array;
                    }
                    expect(',');
                }
            }

            private String parseString() {
                expect('"');
                StringBuilder builder = new StringBuilder();
                while (!isAtEnd()) {
                    char current = text.charAt(position++);
                    if (current == '"') {
                        return builder.toString();
                    }
                    if (current == '\\') {
                        builder.append(parseEscape());
                    } else {
                        builder.append(current);
                    }
                }
                throw new JsonException("Unterminated string.");
            }

            private char parseEscape() {
                if (isAtEnd()) {
                    throw new JsonException("Unterminated escape.");
                }
                char escaped = text.charAt(position++);
                return switch (escaped) {
                    case '"' -> '"';
                    case '\\' -> '\\';
                    case '/' -> '/';
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case 'u' -> parseUnicode();
                    default -> throw new JsonException("Invalid escape: " + escaped);
                };
            }

            private char parseUnicode() {
                if (position + 4 > text.length()) {
                    throw new JsonException("Invalid unicode escape.");
                }
                String hex = text.substring(position, position + 4);
                position += 4;
                try {
                    return (char) Integer.parseInt(hex, 16);
                } catch (NumberFormatException exception) {
                    throw new JsonException("Invalid unicode escape.", exception);
                }
            }

            private Object parseLiteral(String literal, Object value) {
                if (!text.startsWith(literal, position)) {
                    throw new JsonException("Invalid literal.");
                }
                position += literal.length();
                return value;
            }

            private Number parseNumber() {
                int start = position;
                if (consume('-')) {
                    // Sign consumed.
                }
                readDigits();
                boolean fractional = false;
                if (consume('.')) {
                    fractional = true;
                    readDigits();
                }
                if (consume('e') || consume('E')) {
                    fractional = true;
                    if (consume('+') || consume('-')) {
                        // Exponent sign consumed.
                    }
                    readDigits();
                }
                String number = text.substring(start, position);
                try {
                    if (fractional) {
                        return Double.parseDouble(number);
                    }
                    return new BigDecimal(number).longValueExact();
                } catch (ArithmeticException | NumberFormatException exception) {
                    try {
                        return Double.parseDouble(number);
                    } catch (NumberFormatException ignored) {
                        throw new JsonException("Invalid number.", exception);
                    }
                }
            }

            private void readDigits() {
                int start = position;
                while (!isAtEnd() && Character.isDigit(text.charAt(position))) {
                    position++;
                }
                if (position == start) {
                    throw new JsonException("Expected digit.");
                }
            }

            private void skipWhitespace() {
                while (!isAtEnd()) {
                    char current = text.charAt(position);
                    if (current == ' ' || current == '\n' || current == '\r' || current == '\t') {
                        position++;
                    } else {
                        return;
                    }
                }
            }

            private boolean consume(char expected) {
                if (!isAtEnd() && text.charAt(position) == expected) {
                    position++;
                    return true;
                }
                return false;
            }

            private void expect(char expected) {
                if (!consume(expected)) {
                    throw new JsonException("Expected '" + expected + "'.");
                }
            }

            private boolean isAtEnd() {
                return position >= text.length();
            }
        }
    }

    private static final class JsonException extends RuntimeException {
        private JsonException(String message) {
            super(message);
        }

        private JsonException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

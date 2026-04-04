const chatInput = document.getElementById("chat-input");
const timeline = document.getElementById("chat-timeline");
const sendBtn = document.getElementById("send-btn");
const clearBtn = document.getElementById("clear-btn");

document.querySelectorAll(".preset-chip").forEach(button => {
    button.addEventListener("click", () => {
        chatInput.value = button.dataset.preset || "";
        chatInput.focus();
    });
});

sendBtn.addEventListener("click", () => handleAutoRoute());
clearBtn.addEventListener("click", clearConversation);
chatInput.addEventListener("keydown", event => {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        handleAutoRoute();
    }
});

async function handleAutoRoute() {
    const text = currentInput();
    if (!text) {
        return;
    }

    if (looksLikeSkillRequest(text)) {
        await handleSkillRequest();
        return;
    }

    await handlePlanRequest();
}

async function handlePlanRequest() {
    const text = currentInput();
    if (!text) {
        return;
    }

    addUserMessage(text);
    chatInput.value = "";

    await withButtonsDisabled("正在生成方案...", async () => {
        addAssistantMessage("我先把你的自然语言需求直接交给后端工作流，让 Requirement Agent 去做结构化，再生成最终装修方案。");
        const result = await postJson("/api/plans/generate", buildPlanPayload(text));
        addAssistantHtml(renderPlanConversation(result));
    });
}

async function handleSkillRequest() {
    const text = currentInput();
    if (!text) {
        return;
    }

    addUserMessage(text);
    chatInput.value = "";

    await withButtonsDisabled("正在执行 Skill...", async () => {
        addAssistantMessage("我会先判断这句话更像线下门店搜索还是线上商品搜索，再走对应的 skill 和 MCP。");
        const result = await postJson("/api/skills/execute", inferSkillPayload(text));
        addAssistantHtml(renderSkillConversation(result));
    });
}

function buildPlanPayload(text) {
    return {
        sessionId: getOrCreateSessionId(),
        rawDescription: text,
        familyMembers: inferFamilyMembers(text),
        specialNeeds: inferSpecialNeeds(text),
        stylePreference: inferStylePreference(text),
        houseType: inferHouseType(text),
        area: inferArea(text),
        budget: inferBudget(text)
    };
}

function inferSkillPayload(text) {
    return {
        userRequest: text,
        location: inferLocation(text),
        materialKeyword: inferMaterialKeyword(text),
        platform: inferPlatform(text),
        furnitureKeyword: inferFurnitureKeyword(text)
    };
}

function looksLikeSkillRequest(text) {
    return containsAny(text, ["哪里买", "附近", "门店", "建材市场", "京东", "淘宝", "搜几款", "候选商品", "商品"]);
}

function inferLocation(text) {
    const districtMatch = text.match(/(北京|上海|天津|重庆)?[^，。,；;]*?(区|县|镇)/);
    if (districtMatch) {
        return districtMatch[0].replace(/(什么地方|哪里|能买|帮我看看)/g, "").trim();
    }

    const cityMatch = text.match(/(北京|上海|天津|重庆|[^\s，。,；;]{2,8}市)/);
    return cityMatch ? cityMatch[0].trim() : "上海";
}

function inferMaterialKeyword(text) {
    const keywords = ["防滑地砖", "瓷砖", "地砖", "乳胶漆", "板材", "地板", "灯具", "五金"];
    return keywords.find(keyword => text.includes(keyword)) || "建材";
}

function inferPlatform(text) {
    if (containsAny(text, ["京东", "jd", "JD"])) {
        return "jd";
    }
    return "jd";
}

function inferFurnitureKeyword(text) {
    const candidates = ["现代简约沙发", "沙发", "餐桌", "床", "衣柜", "书柜", "椅子", "落地灯"];
    return candidates.find(keyword => text.includes(keyword))
        || text.replace(/帮我|在京东|搜几款|给我几个候选商品|。/g, "").trim();
}

function inferFamilyMembers(text) {
    const members = [];
    if (containsAny(text, ["夫妻", "两口子"])) members.push("夫妻");
    if (containsAny(text, ["孩子", "儿童", "儿子", "女儿"])) members.push("孩子");
    if (containsAny(text, ["宠物", "猫", "狗"])) members.push("宠物");
    if (containsAny(text, ["老人", "父母", "老年"])) members.push("老人");
    return members;
}

function inferSpecialNeeds(text) {
    const mapping = ["收纳", "安全", "清洁", "防滑", "耐脏", "耐磨", "宠物友好"];
    return mapping.filter(item => text.includes(item));
}

function inferStylePreference(text) {
    const styles = ["现代简约", "现代原木", "原木风", "奶油风", "中古风", "极简风", "北欧风"];
    return styles.find(style => text.includes(style)) || "";
}

function inferHouseType(text) {
    const match = text.match(/([一二三四五六七八九十0-9]+室[一二三四五六七八九十0-9]*厅?)/);
    return match ? match[0] : "";
}

function inferArea(text) {
    const match = text.match(/(\d{2,3})\s*平/);
    return match ? Number(match[1]) : null;
}

function inferBudget(text) {
    const wanMatch = text.match(/(\d+(?:\.\d+)?)\s*万/);
    if (wanMatch) {
        return String(Number(wanMatch[1]) * 10000);
    }
    const plainMatch = text.match(/预算[^0-9]*(\d{4,8})/);
    return plainMatch ? plainMatch[1] : null;
}

function addUserMessage(text) {
    timeline.appendChild(buildMessage("user", "用户请求", `<p>${escapeHtml(text)}</p>`));
    scrollToBottom();
}

function addAssistantMessage(text) {
    timeline.appendChild(buildMessage("assistant", "系统反馈", `<p>${escapeHtml(text)}</p>`));
    scrollToBottom();
}

function addAssistantHtml(html) {
    timeline.appendChild(buildMessage("assistant", "系统结果", html));
    scrollToBottom();
}

function buildMessage(role, label, html) {
    const article = document.createElement("article");
    article.className = `message ${role}`;
    article.innerHTML = `
        <div class="avatar">${role === "user" ? "你" : "AI"}</div>
        <div class="bubble">
            <p class="message-label">${label}</p>
            ${html}
        </div>
    `;
    return article;
}

function renderPlanConversation(plan) {
    const conflicts = Array.isArray(plan.conflicts) ? plan.conflicts : [];
    const alternatives = Array.isArray(plan.alternativeOptions) ? plan.alternativeOptions : [];

    return `
        <div class="result-card">
            <h3>整体摘要</h3>
            <p>${escapeHtml(plan.summary || "暂无摘要")}</p>
        </div>
        <div class="result-card">
            <h3>主方案</h3>
            ${renderPlanOption(plan.primaryOption)}
        </div>
        <div class="result-card">
            <h3>备选方案</h3>
            ${alternatives.length ? alternatives.map(renderPlanOption).join("") : `<p>暂无备选方案</p>`}
        </div>
        <div class="result-card">
            <h3>冲突协调</h3>
            ${conflicts.length ? conflicts.map(renderConflict).join("") : `<p>暂无冲突项</p>`}
        </div>
        <div class="result-card">
            <h3>决策理由</h3>
            <p>${escapeHtml(plan.decisionReason || "暂无决策理由")}</p>
        </div>
    `;
}

function renderPlanOption(option) {
    if (!option) {
        return "<p>暂无方案数据</p>";
    }

    return `
        <div class="result-card">
            <div class="pill-row">
                <span class="pill">${escapeHtml(option.name || "未命名方案")}</span>
                ${option.positioning ? `<span class="pill warm">${escapeHtml(option.positioning)}</span>` : ""}
            </div>
            ${renderList("建议", option.recommendations)}
            ${renderList("优点", option.advantages)}
            ${renderList("缺点", option.disadvantages)}
            ${option.applicableCrowd ? `<p>适用场景：${escapeHtml(option.applicableCrowd)}</p>` : ""}
        </div>
    `;
}

function renderConflict(conflict) {
    return `
        <div class="result-card">
            <div class="pill-row">
                <span class="pill">${escapeHtml(conflict.topic || "冲突主题")}</span>
                ${conflict.severity ? `<span class="pill warm">${escapeHtml(conflict.severity)}</span>` : ""}
            </div>
            <p>${escapeHtml(conflict.description || "暂无冲突描述")}</p>
            ${renderSimpleLine("取舍点", conflict.tradeOff)}
            ${renderSimpleLine("最终方向", conflict.chosenDirection)}
            ${renderSimpleLine("解决建议", conflict.resolution)}
        </div>
    `;
}

function renderSkillConversation(result) {
    const stores = Array.isArray(result.materialStoreRecommendations) ? result.materialStoreRecommendations : [];
    const furniture = Array.isArray(result.furnitureSearchResults) ? result.furnitureSearchResults : [];

    return `
        <div class="result-card">
            <h3>路由结果</h3>
            <div class="pill-row">
                <span class="pill">${result.triggered ? "已触发 Skill" : "未触发 Skill"}</span>
                ${result.skillName ? `<span class="pill warm">${escapeHtml(result.skillName)}</span>` : ""}
                ${result.mcpService ? `<span class="pill">${escapeHtml(result.mcpService)}</span>` : ""}
            </div>
            <p>${escapeHtml(result.message || "暂无消息")}</p>
        </div>
        <div class="result-card">
            <h3>Skill Prompt 预览</h3>
            <pre>${escapeHtml(preview(result.skillPrompt))}</pre>
        </div>
        <div class="result-card">
            <h3>线下建材门店</h3>
            ${stores.length ? `
                <ul class="list">
                    ${stores.map(store => `
                        <li>
                            <strong>${escapeHtml(store.storeName || "未命名门店")}</strong><br>
                            ${escapeHtml(store.address || "暂无地址")}
                            ${store.distance ? `<br>距离：${escapeHtml(store.distance)}` : ""}
                        </li>
                    `).join("")}
                </ul>
            ` : `<p>暂无门店结果</p>`}
        </div>
        <div class="result-card">
            <h3>线上家具候选</h3>
            ${furniture.length ? `
                <ul class="list">
                    ${furniture.map(item => `
                        <li>
                            <strong>${escapeHtml(item.title || "未命名商品")}</strong>
                            ${item.price ? `<br>价格：${escapeHtml(item.price)}` : ""}
                            ${item.shopName ? `<br>店铺：${escapeHtml(item.shopName)}` : ""}
                            ${item.link ? `<br><a href="${escapeHtml(item.link)}" target="_blank" rel="noreferrer">查看链接</a>` : ""}
                        </li>
                    `).join("")}
                </ul>
            ` : `<p>暂无商品结果</p>`}
        </div>
    `;
}

function renderList(title, items) {
    if (!Array.isArray(items) || !items.length) {
        return "";
    }
    return `
        <h3>${escapeHtml(title)}</h3>
        <ul class="list">
            ${items.map(item => `<li>${escapeHtml(item)}</li>`).join("")}
        </ul>
    `;
}

function renderSimpleLine(label, value) {
    if (!value) {
        return "";
    }
    return `<p><strong>${escapeHtml(label)}：</strong>${escapeHtml(value)}</p>`;
}

function preview(text) {
    if (!text) {
        return "暂无 skill prompt";
    }
    return text.length <= 360 ? text : `${text.slice(0, 360)}...`;
}

async function postJson(url, payload) {
    const response = await fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
    });

    if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Request failed: ${response.status}`);
    }

    return response.json();
}

async function withButtonsDisabled(statusText, action) {
    const buttons = [sendBtn];
    const originalTexts = buttons.map(button => button.textContent);
    buttons.forEach((button, index) => {
        button.disabled = true;
        button.textContent = statusText;
    });

    try {
        await action();
    } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        addAssistantMessage(`请求失败：${message}`);
    } finally {
        buttons.forEach((button, index) => {
            button.disabled = false;
            button.textContent = originalTexts[index];
        });
    }
}

function clearConversation() {
    resetSessionId();
    timeline.innerHTML = `
        <article class="message assistant">
            <div class="avatar">AI</div>
            <div class="bubble">
                <p class="message-label">系统提示</p>
                <p>直接输入一句话就可以开始。我会根据内容决定是走装修方案生成，还是走 skill + MCP 外部能力。</p>
            </div>
        </article>
    `;
}

function scrollToBottom() {
    timeline.scrollTop = timeline.scrollHeight;
}

function currentInput() {
    return chatInput.value.trim();
}

function containsAny(text, keywords) {
    return keywords.some(keyword => text.includes(keyword));
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}


function getOrCreateSessionId() {
    const key = 'yzan-chat-session-id';
    let sessionId = window.localStorage.getItem(key);
    if (!sessionId) {
        sessionId = (window.crypto?.randomUUID?.() || "session-" + Date.now());
        window.localStorage.setItem(key, sessionId);
    }
    return sessionId;
}

function resetSessionId() {
    window.localStorage.removeItem('yzan-chat-session-id');
}


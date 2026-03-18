// ============ CHATBOT FUNCTIONS ============
let CHATBOT = {
    currentSessionId: null,
    userId: null,
    isInitialized: false
};

// Khởi tạo chatbot khi tab được load
function initChatbot() {
    if (!$('#chatMessages').length) return;

    // Lấy userId từ APP.userInfo
    if (APP.userInfo && APP.userInfo.id) {
        CHATBOT.userId = APP.userInfo.id;
        const storageKey = 'chatbot_session_user_' + CHATBOT.userId;
        let savedSession = localStorage.getItem(storageKey);
        if (!savedSession) {
            savedSession = 'user_' + CHATBOT.userId + '_session_' + Date.now();
            localStorage.setItem(storageKey, savedSession);
        }
        CHATBOT.currentSessionId = savedSession;
    } else {
        const storageKey = 'chatbot_session_guest';
        let savedSession = localStorage.getItem(storageKey);
        if (!savedSession) {
            savedSession = 'chat_' + Date.now();
            localStorage.setItem(storageKey, savedSession);
        }
        CHATBOT.currentSessionId = savedSession;
    }

    loadChatHistory();
    CHATBOT.isInitialized = true;
}

// Bắt đầu cuộc hội thoại mới
function startNewChat() {
    if (!confirm('Bắt đầu cuộc hội thoại mới? Lịch sử hiện tại sẽ không bị xóa.')) return;

    const newSessionId = CHATBOT.userId
        ? 'user_' + CHATBOT.userId + '_session_' + Date.now()
        : 'chat_' + Date.now();

    const storageKey = CHATBOT.userId
        ? 'chatbot_session_user_' + CHATBOT.userId
        : 'chatbot_session_guest';

    localStorage.setItem(storageKey, newSessionId);
    CHATBOT.currentSessionId = newSessionId;

    showWelcomeScreen();
    updateChatCount();
}

// Hiển thị màn hình chào mừng
function showWelcomeScreen() {
    const chatMessages = $('#chatMessages');
    chatMessages.empty();
    chatMessages.html(`
        <div class="welcome-screen" id="welcomeScreen">
            <div class="welcome-avatar">🤖</div>
            <div class="welcome-title">Xin chào! Tôi là EduBot</div>
            <p class="welcome-sub">
                Tôi có thể giúp bạn tra cứu thông tin chương trình đào tạo. Hãy bắt đầu bằng cách đặt câu hỏi!
            </p>
            <div class="suggestion-chips">
                <div class="suggestion-chip" onclick="suggestQuestion('Có những môn học nào trong học kỳ 1?')">📚 Môn học học kỳ 1</div>
                <div class="suggestion-chip" onclick="suggestQuestion('Tổng số tín chỉ toàn khóa là bao nhiêu?')">🎯 Tổng số tín chỉ</div>
                <div class="suggestion-chip" onclick="suggestQuestion('Có những môn tự chọn nào?')">🔀 Môn tự chọn</div>
                <div class="suggestion-chip" onclick="suggestQuestion('Liệt kê các môn học bắt buộc')">✅ Môn bắt buộc</div>
            </div>
        </div>
    `);
}

// Gửi tin nhắn
function sendMessage() {
    const input = $('#chatInput');
    const message = input.val().trim();
    if (!message) return;

    $('#welcomeScreen').remove();
    addMessageToChat('user', message);

    input.val('');
    input.css('height', 'auto');
    input.focus();
    $('#sendBtn').prop('disabled', true);

    showTypingIndicator();

    $.ajax({
        url: '/api/chat/send',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            sessionId: CHATBOT.currentSessionId,
            message: message
        }),
        success: function(response) {
            hideTypingIndicator();
            console.log("API response:", response);

            // TRUYỀN NGUYÊN OBJECT
            addMessageToChat('ai', response);

            updateChatCount();
            $('#sendBtn').prop('disabled', false);
        },
        error: function(xhr, status, error) {
            hideTypingIndicator();
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : (xhr.responseText || error);
            addMessageToChat('ai', 'Xin lỗi, đã xảy ra lỗi: ' + errorMsg);
            $('#sendBtn').prop('disabled', false);
        }
    });
}

// Xử lý phím Enter / Shift+Enter
function handleChatKeydown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

// Tự động resize textarea
function autoResizeTextarea(el) {
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 120) + 'px';
}

// Typing indicator
function showTypingIndicator() {
    const chatMessages = $('#chatMessages');
    chatMessages.append(`
        <div class="message-row ai" id="typingIndicator">
            <div class="msg-avatar ai">🤖</div>
            <div class="msg-body">
                <div class="msg-bubble ai">
                    <div class="typing-indicator">
                        <div class="typing-dot"></div>
                        <div class="typing-dot"></div>
                        <div class="typing-dot"></div>
                    </div>
                </div>
            </div>
        </div>
    `);
    scrollToBottom();
}

function hideTypingIndicator() {
    $('#typingIndicator').remove();
}

// ===== CORE: Thêm tin nhắn vào giao diện =====
function addMessageToChat(sender, message) {
    const chatMessages = $('#chatMessages');
    if (!chatMessages.length) return;

    const timestamp = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    const isUser = sender === 'user';

    const renderedContent = isUser
        ? escapeHtml(message).replace(/\n/g, '<br>')
        : renderAIResponse(message);

    chatMessages.append(`
        <div class="message-row ${isUser ? 'user' : 'ai'}">
            <div class="msg-avatar ${isUser ? 'user' : 'ai'}">
                ${isUser ? '<i class="fas fa-user"></i>' : '🤖'}
            </div>
            <div class="msg-body">
                <div class="msg-meta">
                    <span class="msg-sender">${isUser ? 'Bạn' : 'EduBot'}</span>
                    <span class="msg-time">${timestamp}</span>
                </div>
                <div class="msg-bubble ${isUser ? 'user' : 'ai'}">
                    <div class="${isUser ? '' : 'ai-response-block'}">${renderedContent}</div>
                </div>
            </div>
        </div>
    `);

    scrollToBottom();
}

// ===== AI RESPONSE RENDERER (ĐÃ SỬA) =====
function renderAIResponse(raw) {

    let data;

    if (typeof raw === "string") {
        try {
            data = JSON.parse(raw);
            if (typeof data === "string") {
                data = JSON.parse(data);
            }
        } catch {
            return renderMarkdown(raw);
        }
    } else {
        data = raw;
    }

    if (!data || typeof data !== "object") {
        return "";
    }

    if (data.raw && typeof data.raw === "object") {
        data = data.raw;
    }

    let html = "";

    // ai_content
    if (typeof data.ai_content === "string" && data.ai_content.trim() !== "") {
        html += renderMarkdown(data.ai_content);
    }

    // ===== THÊM ĐOẠN NÀY =====
    if (Array.isArray(data.mh) && data.mh.length > 0) {

        html += `<b>Các môn học tìm được:</b><br>`;
        html += `<ul style="padding-left:16px;margin:6px 0">`;

        data.mh.forEach(m => {
            if (m.name) {
                html += `<li>
                    ${escapeHtml(m.name)}
                    (${m.code || ""} - ${m.hk || ""})
                </li>`;
            }
        });

        html += `</ul>`;
    }
    // =========================

    // LH
    if (Array.isArray(data.LH) && data.LH.length > 0) {
        html += renderLHCollapse(data.LH);
    }

    return html;
}

// ===============================
// RENDER CHI TIẾT 1 MÔN HỌC
// ===============================
function renderLHItemHTML(item) {

    let html = "";

    if (item["Loại HP"] === "Lý thuyết") {
        html += `<b>${item["Tên HP"]}</b><br>`;
        html += `LT: ${item["Thứ"]} (${item["Tiết Học"]}) – ${item["Phòng"]}<br>`;
    }

    if (item["Loại HP"] === "Thực hành") {
        html += `• TH ${item["Mã LHP"]} – ${item["Thứ"]} (${item["Tiết Học"]}) – ${item["Phòng"]}<br>`;
    }

    return html;
}

// ===============================
// RENDER LH THU GỌN / XEM THÊM
// ===============================
function renderLHCollapse(dataLH) {

    let html = `<div class="lh-block">`;

    html += `<b>Lịch các môn học tìm được</b><br>`;
    html += `<ul style="padding-left:16px; margin:6px 0">`;

    const uniqueSubjects = [...new Set(dataLH.map(i => i["Tên HP"]))];

    uniqueSubjects.forEach(name => {
        html += `<li>${name}</li>`;
    });

    html += `</ul>`;
    html += `<a href="#" data-toggle-lh style="text-decoration:underline">Xem thêm</a>`;
    html += `<div data-lh-detail class="lh-detail" style="margin-top:8px">`;

    const grouped = {};

    dataLH.forEach(item => {
        const subject = item["Tên HP"];

        if (!grouped[subject]) {
            grouped[subject] = { lt: null, th: [] };
        }

        if (item["Loại HP"] === "Lý thuyết") {
            grouped[subject].lt = item;
        }

        if (item["Loại HP"] === "Thực hành") {
            grouped[subject].th.push(item);
        }
    });

    Object.keys(grouped).forEach((subject, index) => {

        const g = grouped[subject];

        html += `<b>${subject}</b><br>`;

        if (g.lt) {
            html += `LT: ${g.lt["Thứ"]} (${g.lt["Tiết Học"]}) – ${g.lt["Phòng"]}<br>`;
        }

        if (g.th.length > 0) {
            g.th.forEach(p => {
                html += `• TH ${p["Mã LHP"]} – ${p["Thứ"]} (${p["Tiết Học"]}) – ${p["Phòng"]}<br>`;
            });
        }

        if (index < Object.keys(grouped).length - 1) {
            html += `<hr style="opacity:0.3">`;
        }

    });

    html += `</div></div>`;

    return html;
}

function renderMarkdown(text) {
    if (!text) return '';
    let html = escapeHtml(text);

    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
    html = html.replace(/^- (.+)$/gm, '<li>$1</li>');
    html = html.replace(/(<li>[\s\S]*?<\/li>)/g, '<ul style="padding-left:16px;margin:4px 0">$1</ul>');
    html = html.replace(/\n/g, '<br>');

    return `<div>${html}</div>`;
}

function escapeHtml(text) {
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function scrollToBottom() {
    const chatMessages = $('#chatMessages');
    chatMessages.scrollTop(chatMessages[0].scrollHeight);
}

// Tải lịch sử chat
function loadChatHistory() {
    if (!CHATBOT.currentSessionId) return;

    $.ajax({
        url: '/api/chat/history/' + CHATBOT.currentSessionId,
        type: 'GET',
        success: function(messages) {

            const chatMessages = $('#chatMessages');
            chatMessages.empty();

            if (messages && messages.length > 0) {
                messages.forEach(msg => {
                    console.log("history message:", msg.message);
                    addMessageToChat(msg.sender.toLowerCase(), msg.message);
                });
                updateChatCount();
            } else {
                showWelcomeScreen();
            }
        },
        error: function() {
            showWelcomeScreen();
        }
    });
}

// Xóa lịch sử chat
function clearChatHistory() {
    if (!confirm('Bạn có chắc chắn muốn xóa toàn bộ lịch sử chat?')) return;

    $.ajax({
        url: '/api/chat/clear/' + CHATBOT.currentSessionId,
        type: 'DELETE',
        success: function() {
            showWelcomeScreen();
            updateChatCount();
        }
    });
}

// Gợi ý câu hỏi
function suggestQuestion(question) {
    const chatInput = $('#chatInput');
    chatInput.val(question).focus();
    autoResizeTextarea(chatInput[0]);
}

// Cập nhật số lượng tin nhắn
function updateChatCount() {
    const count = $('#chatMessages').find('.message-row').length;
    $('#chatCount').text(count);
}

// Hook vào loadTab
$(document).ready(function() {
    const originalLoadTabContent = loadTabContent;
    loadTabContent = function(tabName) {
        originalLoadTabContent(tabName);
        if (tabName === 'chatbot') {
            setTimeout(initChatbot, 300);
        }
    };
});
// Toggle xem thêm / thu gọn
$(document).on("click", "[data-toggle-lh]", function (e) {
    e.preventDefault();

    const block = $(this).closest(".lh-block");
    const detail = block.find("[data-lh-detail]").first();

    detail.toggleClass("open");

    const isOpen = detail.hasClass("open");
    $(this).text(isOpen ? "Thu gọn" : "Xem thêm");
});

APP.chatbot = CHATBOT;
window.APP = APP;
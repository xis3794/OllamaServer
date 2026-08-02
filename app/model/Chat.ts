interface ToolFunction {
    name: string,
    arguments: object
}

interface ToolCall {
    function: ToolFunction
}

interface Message {
    role: string,
    content: string,
    images?: string[], // base64 编码的图片（视觉模型）
}

interface LoadResponse {
    model: string,
    create_at: string,
    message: Message,
    done_reason: string,
    done: boolean
}

interface ChatResponse {
    model: string,
    create_at: string,
    message: Message | null,
    done: boolean,
    total_duration: number | null,
    load_duration: number | null,
    prompt_eval_count: number | null,
    prompt_eval_duration: number | null,
    eval_count: number | null,
    eval_duration: number | null,
    error?: Error;
}

type ChatSessionType = {
    promise: Promise<void>;
    abort: () => void;
};

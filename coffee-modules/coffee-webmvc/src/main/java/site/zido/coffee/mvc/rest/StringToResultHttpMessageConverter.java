package site.zido.coffee.mvc.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * 为string类型返回值定制的消息处理器，配合补充{@link GlobalResultHandler}。
 * <p>
 * 默认spring对string的处理经过GlobalResultHandler之后会返回text/plain类型的json对象,在这个消息转换器中，专门处理string类型，
 * 给前端正确的application/json响应头。
 *
 * @author zido
 */
public class StringToResultHttpMessageConverter extends AbstractHttpMessageConverter<Object> {
    private final ObjectMapper mapper;

    /**
     * 不使用utf-8 charset参数的原因参考{@link MediaType#APPLICATION_JSON_UTF8}
     * @param mapper json mapper，用于序列化对象
     */
    public StringToResultHttpMessageConverter(ObjectMapper mapper) {
        super(MediaType.APPLICATION_JSON, MediaType.ALL);
        this.mapper = mapper;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return String.class == clazz;
    }

    @Override
    protected String readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        Charset charset = getContentTypeCharset(inputMessage.getHeaders().getContentType());
        return StreamUtils.copyToString(inputMessage.getBody(), charset);
    }

    @Override
    protected void writeInternal(Object o, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        Charset charset = getContentTypeCharset(outputMessage.getHeaders().getContentType());
        if (o instanceof String) {
            outputMessage.getBody().write(((String) o).getBytes(charset));
        } else {
            outputMessage.getBody().write(mapper.writeValueAsBytes(o));
        }
    }

    private Charset getContentTypeCharset(MediaType contentType) {
        if (contentType != null && contentType.getCharset() != null) {
            return contentType.getCharset();
        } else {
            return getDefaultCharset();
        }
    }
}

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

// La codificación UTF-8 ayuda a no tener problemas con caracteres especiales
// El filtro se aplicará en todas las rutas de la web
@WebFilter("/*")
public class UTF8Encoding implements Filter {

    @Override
    public void init(FilterConfig filterConfig) 
	   throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
	// Los datos de entrada se interpretarán en UTF-8 
        request.setCharacterEncoding("UTF-8");
	// Los datos de respuesta se enviarán en UTF-8
        response.setCharacterEncoding("UTF-8");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}

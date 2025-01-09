package com.example;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Map;
import java.util.HashMap;

@WebServlet({"/", "/checkout", "/result"})
public class McwServlet extends HttpServlet {
    
    // Componentes principales para la plantilla, las propiedades y el controlador
    private TemplateEngine templateEngine;
    private McwProperties properties;
    private McwController mcwController;

    @Override
    public void init() throws ServletException {
	// Configuración para las plantillas Thymeleaf
        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(getServletContext());
        templateResolver.setPrefix("/WEB-INF/templates/"); // Ubicación de las pantillas
        templateResolver.setSuffix(".html"); // Extensión que tienen los archivos para la plantilla
	templateResolver.setCharacterEncoding("UTF-8"); // Codificación de los caracteres
        templateResolver.setTemplateMode("HTML"); // Modo de procesamiento de plantillas
	
	// Iniciando Thymeleaf
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
	
	// Iniciando las propiedades y el controlador
	properties = new McwProperties();
	mcwController = new McwController();
    }
    

    /**
     * @@ Manejo de rutas GET @@
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
	// Obtiene la ruta solicitada
        String path = request.getServletPath();
        // Creación del contexto para pasar los valores para la plantilla
	WebContext context = new WebContext(request, response, getServletContext());
	
	// Generar orderId
	String orderId = mcwController.generateOrderId();
        
	// Procesamiento según la ruta solicitada
	switch (path) {
	    // Renderiza la plantilla 'index' al solicitar la ruta raíz, checkout y result
            case "/":
		// Agregando el orderId al contexto
		context.setVariable("orderId", orderId);
		// Renderizando el template y enviando los datos agregados al contexto
                templateEngine.process("index", context, response.getWriter());
                break;
            case "/checkout":
                templateEngine.process("index", context, response.getWriter());
                break;
            case "/result":
                templateEngine.process("index", context, response.getWriter());
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                break;
        }
    }


    /**
     * @@ Manejo de rutas POST @@
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
	// Obtiene la ruta solicitada
        String path = request.getServletPath();
	// Creación del contexto para pasar los valores para la plantilla
        WebContext context = new WebContext(request, response, getServletContext());
	
	// Definición de las variables a usar
	String krHash = null;
    	String krHashAlgorithm = null;
    	String krAnswerType = null;
    	String krAnswer = null;
    	String krHashKey = null;
	String orderStatus = null;
	String orderId = null;
	JSONObject jsonResponse = null;	
	
        switch (path) {
            case "/checkout":
		// Procesando datos POST enviados de la ruta raíz y almacenándolos en un Map
		
		Map<String, String> parameters = new HashMap<>();
            	for (String param : new String[]{"firstName", "lastName", "email", "phoneNumber", "identityType", 
                                              "identityCode", "address", "country", "state", "city", "zipCode", 
                                              "orderId", "amount", "currency"}) {
                	parameters.put(param, request.getParameter(param));
            	}
		
		//Obtener PublicKey
		String publicKey = properties.getProperty("PUBLIC_KEY");

		//Obtenemos el FormToken generado
		String formToken = mcwController.generateFormToken(parameters);
		
		//Agrerar el token en el contexto para ser usado en el template
		context.setVariable("formToken", formToken);
		context.setVariable("publicKey", publicKey);
		
		// Renderizando el template y enviando los datos agregados al contexto
		templateEngine.process("checkout", context, response.getWriter());	

		break;		

            case "/result":

		String HMAC_SHA256 = properties.getProperty("HMAC_SHA256");
		
		// Asignando los valores de la respuesta de Izipay en las variables
		krHash = request.getParameter("kr-hash");
        	krHashAlgorithm = request.getParameter("kr-hash-algorithm");
        	krAnswerType = request.getParameter("kr-answer-type");
        	krAnswer = request.getParameter("kr-answer");
        	krHashKey = request.getParameter("kr-hash-key");
		
		// Válida que la respuesta sea íntegra comprando el hash recibido en el 'kr-hash' con el generado con el 'kr-answer'
		if (!mcwController.checkHash(krHash, HMAC_SHA256, krAnswer)){
			break;
		}

		// Almacenamos los datos del kr-answer
		jsonResponse = new JSONObject(krAnswer);
		String pJson = jsonResponse.toString(4);
		
		// Almacenamos los datos del pago en las variables
		orderStatus = jsonResponse.getString("orderStatus");
        	int orderTotalAmount = jsonResponse.getJSONObject("orderDetails").getInt("orderTotalAmount");
        	orderId = jsonResponse.getJSONObject("orderDetails").getString("orderId");
		String currency = jsonResponse.getJSONObject("orderDetails").getString("orderCurrency");

    		double orderAmountdouble = (double) orderTotalAmount / 100;
    		String orderAmount = String.format("%.02f", orderAmountdouble);

								
		// Establece variables en el contexto para la plantilla	
		context.setVariable("krHash", krHash);
                context.setVariable("krHashAlgorithm", krHashAlgorithm);
                context.setVariable("krAnswerType", krAnswerType);
                context.setVariable("krAnswer", krAnswer);
                context.setVariable("krHashKey", krHashKey);
		context.setVariable("pJson", pJson);
		context.setVariable("orderStatus", orderStatus);
		context.setVariable("orderTotalAmount", orderAmount);
		context.setVariable("orderId", orderId);
		context.setVariable("currency", currency);
			
		// Renderizando el template y enviando los datos agregados al contexto
		templateEngine.process("result", context, response.getWriter());
			
		break;

	    
	    case "/ipn":

		String PASSWORD = properties.getProperty("PASSWORD");

		// Asignando los valores de la respuesta IPN en las variables
		krHash = request.getParameter("kr-hash");
        	krAnswer = request.getParameter("kr-answer");
		
		// Válida que la respuesta sea íntegra comprando el hash recibido en el 'kr-hash' con el generado con el 'kr-answer'
		if (!mcwController.checkHash(krHash, PASSWORD, krAnswer)){
			System.out.println("Notification Error");
		}

		// Procesa la respuesta del pago
		jsonResponse = new JSONObject(krAnswer);
		JSONArray transactionsArray = jsonResponse.getJSONArray("transactions");
		JSONObject transactions = transactionsArray.getJSONObject(0);
		
		// Verifica el orderStatus PAID
		orderStatus = jsonResponse.getString("orderStatus");
		orderId = jsonResponse.getJSONObject("orderDetails").getString("orderId");
		String uuid = transactions.getString("uuid");
		
		// Retornando el OrderStatus
		response.getWriter().write("OK! Order Status is " + orderStatus);

		break;
	    
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                break;
        }
    }
}

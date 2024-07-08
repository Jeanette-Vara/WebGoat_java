package com.example.WebGoatJava.Controller;

import com.example.WebGoatJava.Dto.JwtDto;
import com.example.WebGoatJava.Dto.UserRowMapper;
import com.example.WebGoatJava.Entity.Usuario;
import com.example.WebGoatJava.JWT.JwtUtil;
import com.example.WebGoatJava.Service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Map;

import static com.example.WebGoatJava.JWT.JwtUtil.extractRol;
import static com.example.WebGoatJava.JWT.JwtUtil.extractUser;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = {"Origin", "Content-Type", "Accept", "Authorization"})
public class AuthController {
    @Autowired
    UsuarioService usuarioService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    MD5hashUtil MD5HashUtil;
    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    PasswordEncoder passwordEncoder;

    private final static Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    @PostMapping("/login")
    public ResponseEntity<JwtDto> login(@RequestBody Usuario loginUsuario) {
        try {
            if (loginUsuario.getNombreUsuario().isEmpty() || loginUsuario.getPassword().isEmpty()) {
                return new ResponseEntity(new Mensaje("Los campos no pueden ser nulos"), HttpStatus.BAD_REQUEST);
            }
            String query = "SELECT * FROM usuario WHERE nombre_usuario=?";
            Usuario user = jdbcTemplate.queryForObject(query, new Object[]{loginUsuario.getNombreUsuario()}, new UserRowMapper());
            String hashedPassword = MD5HashUtil.getMD5Hash(loginUsuario.getPassword());
            //   if (user != null && passwordEncoder.matches(loginUsuario.getPassword(), user.getPassword())) {
            if (user != null && (hashedPassword.equals(user.getPassword()))) {
                // La contraseña coincide, puedes generar el token JWT
                String jwt = jwtUtil.generateToken(user);
                JwtDto jwtDto = new JwtDto(jwt);
                logger.info("Inicio de sesión exitoso");
                return new ResponseEntity<>(jwtDto, HttpStatus.OK);
            }
        } catch (EmptyResultDataAccessException ex) {
            // Usuario no encontrado
            // Mensaje mensaje = new Mensaje("Error, campos incorrectos", "error", HttpStatus.BAD_REQUEST.value());
            return new ResponseEntity(new Mensaje("Error, campos incorrectos"), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            // Otro tipo de error
            // Mensaje mensaje = new Mensaje("Ocurrió un error interno", "error", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return new ResponseEntity(new Mensaje("Ocurrió un error interno"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity(new Mensaje("Ocurrió un error, contacte al administrador"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

  @PostMapping("/nuevo")
    public ResponseEntity<?>nuevo(@RequestBody Usuario usuario, @RequestHeader(value = "Authorization", required = false) String tokenActual){
      String token = tokenActual.replace("Bearer ", "");
      String rol = extractRol(token);
        if (token == null || rol != "ROL_ADMIN" || !token.startsWith("Bearer ") ) {
          usuario.setRol("ROL_USER");
      }
      if (usuarioService.existsByNombreUsuario(usuario.getNombreUsuario()))
          return new ResponseEntity(new Mensaje("Ese nombre de usuario ya existe"), HttpStatus.BAD_REQUEST);
      if (usuarioService.existsByEmail(usuario.getEmail()))
          return new ResponseEntity(new Mensaje("El Email ya existe"), HttpStatus.BAD_REQUEST);
      Usuario nuevousuario =
              new Usuario(usuario.getNombre(), usuario.getNombreUsuario(), usuario.getEmail(),
                      MD5hashUtil.getMD5Hash(usuario.getPassword()), usuario.getPregunta(), usuario.getRespuesta(), usuario.getRol());
      usuarioService.save(nuevousuario);
      return new ResponseEntity(new Mensaje("Usuario Creado"), HttpStatus.CREATED);
  }
  @PostMapping("/renovar")
    public ResponseEntity<?>renovar(@RequestBody Usuario usuario){
      Usuario renovar = usuarioService.getByNombreUsuario(usuario.getNombreUsuario());
      if (usuarioService.existsByPregunta(usuario.getPregunta())){
          if (usuarioService.existsByRespuesta(usuario.getRespuesta())) {
              String query = "SELECT * FROM usuario WHERE nombre_usuario=?";
              Usuario user = jdbcTemplate.queryForObject(query, new Object[]{usuario.getNombreUsuario()}, new UserRowMapper());
                  user.setPassword(MD5hashUtil.getMD5Hash(usuario.getPassword()));
                  usuarioService.cambiarpass(user);

                  return new ResponseEntity(new Mensaje("contraseña actualizada"), HttpStatus.OK);
              }
          }
      return new ResponseEntity(new Mensaje("Campos incorrectos"), HttpStatus.BAD_REQUEST);
      }
    @PostMapping("/cambiarpass")
    public ResponseEntity<?>actualizar(@RequestHeader("Authorization") String tokenActual, @RequestBody Map<String, String> request){

        String password = request.get("password");
        String nuevaContrasena = request.get("nuevaPassword");
        String token = tokenActual.replace("Bearer ", "");
        String username = extractUser(token);
        String query = "SELECT * FROM usuario WHERE nombre_usuario=?";
        Usuario user = jdbcTemplate.queryForObject(query, new Object[]{username}, new UserRowMapper());
       // if (user != null && passwordEncoder.matches(password, user.getPassword())){ //Se deshabilita para solventar fallas criptograficas
        if (user.getPassword().equals(MD5hashUtil.getMD5Hash(password))){
            user.setPassword(MD5hashUtil.getMD5Hash(nuevaContrasena));
            usuarioService.cambiarpass(user);

            return new ResponseEntity(new Mensaje("contraseña actualizada"), HttpStatus.OK);
        }

        return new ResponseEntity(new Mensaje("No se logro actualizar, intentelo de nuevo"), HttpStatus.BAD_REQUEST);
    }
    @GetMapping("/perfil")
    public ResponseEntity<?> getPerfil(@RequestHeader("Authorization") String tokenActual) {
        //Extraer el nombre de usuario del token
        String token = tokenActual.replace("Bearer ", "");
        String username = extractUser(token);
        //  Buscar el perfil del usuario en la base de datos
        Usuario usuario = usuarioService.getByNombreUsuario(username);
        if (usuario == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(usuario, HttpStatus.OK);
    }

}

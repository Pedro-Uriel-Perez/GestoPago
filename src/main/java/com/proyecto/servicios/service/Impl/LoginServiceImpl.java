package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Login;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.repositorys.clientes.ClienteRepository;
import com.proyecto.servicios.repositorys.clientes.LoginRepository;
import com.proyecto.servicios.security.JwtService;
import com.proyecto.servicios.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    private final ClienteRepository clienteRepository;
    private final LoginRepository loginRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${security.session.inactividad-minutos}")
    private long inactividadMinutos;

    public LoginServiceImpl(ClienteRepository clienteRepository,
                             LoginRepository loginRepository,
                             JwtService jwtService) {
        this.clienteRepository = clienteRepository;
        this.loginRepository = loginRepository;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public void registrarCredenciales(Cliente cliente, String passwordPlano) {
        Login login = new Login();
        login.setCliente(cliente);
        login.setCorreoLogin(cliente.getCorreoElectronico());
        login.setNombreCifrado(cliente.getNombre());
        login.setPasswordHash(passwordEncoder.encode(passwordPlano));
        login.setSesionActiva(false);
        loginRepository.save(login);
    }

    @Override
    @Transactional
    public String iniciarSesion(String correoElectronico, String passwordPlano) {
        Cliente cliente = clienteRepository.findByCorreoElectronico(correoElectronico)
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contrasena incorrectos"));

        Login login = loginRepository.findByClienteId(cliente.getId())
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contrasena incorrectos"));

        Optional.of(login.getPasswordHash())
                .filter(hash -> passwordEncoder.matches(passwordPlano, hash))
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contrasena incorrectos"));

        String token = jwtService.generarToken(cliente.getId(), cliente.getCorreoElectronico());

        login.setJwtToken(token);
        login.setSesionActiva(true);
        login.setFechaUltimoAcceso(LocalDateTime.now());
        loginRepository.save(login);

        log.info("Sesion iniciada. clienteId={}", cliente.getId());
        return token;
    }

    @Override
    @Transactional
    public void cerrarSesion(Integer clienteId) {
        loginRepository.findByClienteId(clienteId).ifPresent(login -> {
            login.setSesionActiva(false);
            login.setJwtToken(null);
            loginRepository.save(login);
        });
        log.info("Sesion cerrada. clienteId={}", clienteId);
    }

    @Override
    @Scheduled(cron = "${security.session.check-cron:0 * * * * *}")
    @Transactional
    public void cerrarSesionesInactivas() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(inactividadMinutos);
        List<Login> sesionesInactivas = loginRepository.findBySesionActivaTrueAndFechaUltimoAccesoBefore(limite);

        sesionesInactivas.forEach(login -> {
            login.setSesionActiva(false);
            login.setJwtToken(null);
        });
        loginRepository.saveAll(sesionesInactivas);

        log.info("Revision de sesiones inactivas completada. sesiones cerradas={}", sesionesInactivas.size());
    }
}

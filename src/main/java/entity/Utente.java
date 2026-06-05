package entity;

import jakarta.persistence.*;

/*
 * Utente è la superclasse astratta del dominio (livello Entity, BCED).
 * Cliente e Gestore la estendono.
 *
 * Mappatura dell'ereditarietà:
 * usiamo la strategia SINGLE_TABLE, in cui tutte le sottoclassi sono salvate
 * nella stessa tabella. La colonna discriminante è "ruolo": vale "CLIENTE" per
 * i Cliente e "GESTORE" per i Gestore. In questo modo il ruolo associato a
 * ciascun account è gestito direttamente da JPA tramite il discriminatore.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "ruolo", discriminatorType = DiscriminatorType.STRING)
public abstract class Utente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String cognome;

    /*
     * L'email identifica l'account ed è usata in fase di accesso.
     * L'unicità è garantita a livello applicativo dal RegistroUtenti.
     */
    private String email;

    private String telefono;

    /*
     * La password viene memorizzata cifrata (hash SHA-256), mai in chiaro:
     * la cifratura è a carico del RegistroUtenti.
     */
    private String password;

    public Utente() {
    }

    public Utente(String nome, String cognome, String email, String telefono, String password) {
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.telefono = telefono;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", cognome='" + cognome + '\'' +
                ", email='" + email + '\'' +
                ", telefono='" + telefono + '\'' +
                '}';
    }
}

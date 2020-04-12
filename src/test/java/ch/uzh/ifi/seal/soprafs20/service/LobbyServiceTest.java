package ch.uzh.ifi.seal.soprafs20.service;

import ch.uzh.ifi.seal.soprafs20.entity.Lobby;
import ch.uzh.ifi.seal.soprafs20.entity.User;
import ch.uzh.ifi.seal.soprafs20.exceptions.ConflictException;
import ch.uzh.ifi.seal.soprafs20.exceptions.NotFoundException;
import ch.uzh.ifi.seal.soprafs20.exceptions.UnauthorizedException;
import ch.uzh.ifi.seal.soprafs20.repository.LobbyRepository;
import ch.uzh.ifi.seal.soprafs20.rest.dto.LobbyPostDTO;
import ch.uzh.ifi.seal.soprafs20.rest.dto.LobbyPutDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;



public class LobbyServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;

    @InjectMocks
    private LobbyService lobbyService;

    private Lobby testLobby;

    @BeforeEach
    public void setup(){
        MockitoAnnotations.initMocks(this);

        //given
        testLobby = new Lobby();
        testLobby.setLobbyName("BadBunny");
        testLobby.setToken("1");
        testLobby.setNumberOfPlayers(5);
        testLobby.setVoiceChat(false);
        testLobby.setUserId(1L);
        testLobby.setLobbyId(1L);


        // when -> any object is being save in the userRepository -> return the dummy testUser
        Mockito.when(lobbyRepository.save(Mockito.any())).thenReturn(testLobby);
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(java.util.Optional.ofNullable(testLobby));

    }


    @Test
    public void createLobby_validInput_publicLobby(){
        testLobby.setPrivate(false);
        Lobby lobby = lobbyService.createLobby(testLobby);

        Mockito.verify(lobbyRepository,Mockito.times(1)).save(Mockito.any());

        assertEquals(testLobby.getLobbyId(),lobby.getLobbyId());
        assertEquals(testLobby.getLobbyName(),lobby.getLobbyName());
        assertEquals(testLobby.getNumberOfPlayers(),lobby.getNumberOfPlayers());
        assertNull(lobby.getNumberOfBots());
        assertNull(lobby.getPrivateKey());
    }

    @Test
    public void createLobby_validInput_privateLobby(){
        testLobby.setPrivate(true);
        Lobby lobby = lobbyService.createLobby(testLobby);

        Mockito.verify(lobbyRepository,Mockito.times(1)).save(Mockito.any());

        assertEquals(testLobby.getLobbyId(),lobby.getLobbyId());
        assertEquals(testLobby.getLobbyName(),lobby.getLobbyName());
        assertEquals(testLobby.getNumberOfPlayers(),lobby.getNumberOfPlayers());
        assertNull(lobby.getNumberOfBots());
        assertNotNull(lobby.getPrivateKey());
    }

    @Test
    public void updateExistingLobby_validInput(){
        testLobby.setPrivate(false);
        LobbyPutDTO lobbyPutDTO = new LobbyPutDTO();
        lobbyPutDTO.setNumberOfPlayers(3);
        lobbyPutDTO.setNumberOfBots(3);
        lobbyPutDTO.setToken("1");

        Lobby lobby = lobbyService.updateLobby(testLobby,lobbyPutDTO);

        assertEquals(3,lobby.getNumberOfPlayers());
        assertEquals(3,lobby.getNumberOfBots());


    }

    @Test
    public void updateExistingLobby_tooManyPlayers(){
        testLobby.setPrivate(false);
        LobbyPutDTO lobbyPutDTO = new LobbyPutDTO();
        lobbyPutDTO.setNumberOfBots(3);
        lobbyPutDTO.setToken("1");

        assertThrows(ConflictException.class,() ->{lobbyService.updateLobby(testLobby,lobbyPutDTO);});


    }

    @Test
    public void updateExistingLobby_tooLittlePlayers(){
        testLobby.setPrivate(false);
        LobbyPutDTO lobbyPutDTO = new LobbyPutDTO();
        lobbyPutDTO.setNumberOfPlayers(2);
        lobbyPutDTO.setToken("1");

        assertThrows(ConflictException.class,() ->{lobbyService.updateLobby(testLobby,lobbyPutDTO);});
    }

    @Test
    public void updateExistingLobby_unauthorizedUser(){
        testLobby.setPrivate(false);
        LobbyPutDTO lobbyPutDTO = new LobbyPutDTO();
        lobbyPutDTO.setToken("2");

        assertThrows(UnauthorizedException.class,() -> {lobbyService.updateLobby(testLobby,lobbyPutDTO);});
    }

    @Test
    public void getLobby(){
        testLobby.setPrivate(false);
        Lobby lobby = lobbyService.createLobby(testLobby);
        Lobby foundLobby = lobbyService.getLobby(lobby.getLobbyId());

        Mockito.verify(lobbyRepository, Mockito.times(1)).findById(Mockito.any());

        assertEquals(foundLobby.getLobbyId(),lobby.getLobbyId());
        assertEquals(foundLobby.getLobbyName(),lobby.getLobbyName());
        assertEquals(foundLobby.getNumberOfBots(),lobby.getNumberOfBots());
        assertEquals(foundLobby.getNumberOfPlayers(),lobby.getNumberOfPlayers());
        assertEquals(foundLobby.getUserId(),lobby.getUserId());
    }

    @Test
    public void getLobby_wrongId_throwsException(){
        testLobby.setPrivate(false);
        lobbyService.createLobby(testLobby);

        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(null);
        assertThrows(NullPointerException.class,() -> {lobbyService.getLobby(1L);});

    }

    @Test
    public void removePlayerFromLobby_success(){
        //lobby leader
        User user1 = new User();
        user1.setToken("1");
        user1.setId(1L);
        user1.setUsername("Flacko");
        user1.setPassword("1");

        //second user
        User user2 = new User();
        user2.setToken("123");
        user2.setId(3L);
        user2.setUsername("Bunny");
        user2.setPassword("1");

        //lobby setup
        testLobby.setPrivate(false);
        testLobby.setUsersInLobby(user1);
        testLobby.setUsersInLobby(user2);

        //kick list setup
        LobbyPutDTO lobbyPutDTO = new LobbyPutDTO();
        lobbyPutDTO.setToken("1");
        ArrayList<Long> kick = new ArrayList<>();
        kick.add(3L);
        lobbyPutDTO.setUsersToKick(kick);

        assertEquals(testLobby.getUsersInLobby().size(),2);
        lobbyService.updateLobby(testLobby,lobbyPutDTO);

        assertEquals(testLobby.getUsersInLobby().size(),1);
        assertFalse(testLobby.getUsersInLobby().contains(user2));
    }

    @Test
    public void removeLobbyLeaderFromLobby_fail(){
        //lobby leader
        User user1 = new User();
        user1.setToken("1");
        user1.setId(1L);
        user1.setUsername("Flacko");
        user1.setPassword("1");

        //second user
        User user2 = new User();
        user2.setToken("123");
        user2.setId(3L);
        user2.setUsername("Bunny");
        user2.setPassword("1");

        //lobby setup
        testLobby.setPrivate(false);
        testLobby.setUsersInLobby(user1);
        testLobby.setUsersInLobby(user2);

        //kick list setup
        LobbyPutDTO lobbyPutDTO = new LobbyPutDTO();
        lobbyPutDTO.setToken("1");
        ArrayList<Long> kick = new ArrayList<>();
        kick.add(1L);
        lobbyPutDTO.setUsersToKick(kick);

        assertEquals(testLobby.getUsersInLobby().size(),2);

        lobbyService.updateLobby(testLobby,lobbyPutDTO);

        assertEquals(testLobby.getUsersInLobby().size(),2);
        assertTrue(testLobby.getUsersInLobby().contains(user1));
    }
}

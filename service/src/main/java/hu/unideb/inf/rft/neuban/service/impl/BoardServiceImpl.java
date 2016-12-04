package hu.unideb.inf.rft.neuban.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import hu.unideb.inf.rft.neuban.service.interfaces.shared.SingleDataGetService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import hu.unideb.inf.rft.neuban.persistence.entities.BoardEntity;
import hu.unideb.inf.rft.neuban.persistence.repositories.BoardRepository;
import hu.unideb.inf.rft.neuban.service.domain.BoardDto;
import hu.unideb.inf.rft.neuban.service.domain.UserDto;
import hu.unideb.inf.rft.neuban.service.exceptions.BoardNotFoundException;
import hu.unideb.inf.rft.neuban.service.exceptions.NonExistentBoardIdException;
import hu.unideb.inf.rft.neuban.service.exceptions.NonExistentUserIdException;
import hu.unideb.inf.rft.neuban.service.exceptions.RelationNotFoundException;
import hu.unideb.inf.rft.neuban.service.interfaces.BoardService;
import hu.unideb.inf.rft.neuban.service.interfaces.UserService;

import static hu.unideb.inf.rft.neuban.service.configuration.CrudServiceBeanNameProvider.SINGLE_BOARD_DATA_GET_SERVICE;

@Service
public class BoardServiceImpl implements BoardService {

	@Autowired
	private BoardRepository boardRepository;
	@Autowired
	private UserService userService;
	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	@Qualifier(SINGLE_BOARD_DATA_GET_SERVICE)
	private SingleDataGetService<BoardDto, Long> singleBoardDataGetService;

	@Transactional(readOnly = true)
	@Override
	public Optional<BoardDto> get(final Long boardId) {
		return this.singleBoardDataGetService.get(boardId);
	}

	@Transactional(readOnly = true)
	@Override
	public List<BoardDto> getAllByUserId(final Long userId) {
		final Optional<UserDto> userDtoOptional = this.userService.get(userId);

		if (userDtoOptional.isPresent()) {
			return userDtoOptional.get().getBoards();
		}
		return Lists.newArrayList();
	}

	@Transactional
	@Override
	public void update(final BoardDto boardDto) throws BoardNotFoundException {
		Assert.notNull(boardDto);
		Assert.notNull(boardDto.getId());

		final BoardEntity boardEntity = this.modelMapper.map(boardDto, BoardEntity.class);

		this.boardRepository.saveAndFlush(boardEntity);
	}

	@Transactional
	@Override
	public void remove(final Long boardId) throws BoardNotFoundException {
		Assert.notNull(boardId);

		List<UserDto> userDtos = userService.getAll();

		final BoardEntity boardEntity = Optional.ofNullable(this.boardRepository.findOne(boardId))
				.orElseThrow(() -> new BoardNotFoundException(boardId.toString()));

		final BoardDto boardDto = modelMapper.map(boardEntity, BoardDto.class);
		userDtos = userDtos.stream().filter(userDto -> userDto.getBoards().contains(boardDto))
				.collect(Collectors.toList());
		for (UserDto userDtoIter : userDtos) {
			userDtoIter.getBoards().remove(boardDto);
			userService.saveOrUpdate(userDtoIter);
		}
		this.boardRepository.delete(boardId);

	}

	@Transactional
	@Override
	public void removeUserFromBoardByUserIdAndByBoardId(final Long userId, final Long boardId)
			throws NonExistentBoardIdException, RelationNotFoundException, NonExistentUserIdException {

		Assert.notNull(userId);
		Assert.notNull(boardId);

		UserDto userDto = this.userService.get(userId).orElseThrow(() -> new NonExistentUserIdException(userId));

		BoardDto boardDto = get(boardId).orElseThrow(() -> new NonExistentBoardIdException(boardId));

		if (userDto.getBoards() == null
				|| !userDto.getBoards().removeIf(userBoards -> userBoards.getId().equals((boardDto.getId())))) {
			throw new RelationNotFoundException();
		}
		userService.saveOrUpdate(userDto);
	}

	@Transactional
	@Override
	public void addUserToBoardByUserIdAndByBoardId(final Long userId, final Long boardId)
			throws NonExistentBoardIdException, NonExistentUserIdException {

		Assert.notNull(userId);
		Assert.notNull(boardId);

		UserDto userDto = this.userService.get(userId).orElseThrow(() -> new NonExistentUserIdException(userId));

		BoardDto boardDto = get(boardId).orElseThrow(() -> new NonExistentBoardIdException(boardId));

		if (userDto.getBoards() != null) {
			userDto.getBoards().add(boardDto);
		} else {
			userDto.setBoards(Lists.newArrayList(boardDto));
		}

		userService.saveOrUpdate(userDto);

	}

	@Transactional
	@Override
	public void createBoard(final Long userId, final String title) throws NonExistentUserIdException {

		Assert.notNull(userId);
		Assert.notNull(title);

		UserDto userDto = this.userService.get(userId).orElseThrow(() -> new NonExistentUserIdException(userId));

		BoardDto boardDto = BoardDto.builder().title(title).build();

		if (userDto.getBoards() != null) {
			userDto.getBoards().add(boardDto);
		} else {
			userDto.setBoards(Lists.newArrayList(boardDto));
		}
		userService.saveOrUpdate(userDto);
	}

}

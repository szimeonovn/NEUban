package hu.unideb.inf.rft.neuban.service.impl;

import static hu.unideb.inf.rft.neuban.service.provider.beanname.SingleDataGetServiceBeanNameProvider.SINGLE_CARD_DATA_GET_SERVICE;
import static hu.unideb.inf.rft.neuban.service.provider.beanname.SingleDataUpdateServiceBeanNameProvider.SINGLE_CARD_DATA_UPDATE_SERVICE;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import hu.unideb.inf.rft.neuban.persistence.entities.BoardEntity;
import hu.unideb.inf.rft.neuban.persistence.entities.CardEntity;
import hu.unideb.inf.rft.neuban.persistence.entities.ColumnEntity;
import hu.unideb.inf.rft.neuban.persistence.repositories.BoardRepository;
import hu.unideb.inf.rft.neuban.persistence.repositories.CardRepository;
import hu.unideb.inf.rft.neuban.persistence.repositories.ColumnRepository;
import hu.unideb.inf.rft.neuban.service.domain.CardDto;
import hu.unideb.inf.rft.neuban.service.domain.ColumnDto;
import hu.unideb.inf.rft.neuban.service.exceptions.CardAlreadyExistsException;
import hu.unideb.inf.rft.neuban.service.exceptions.ColumnAlreadyExistsException;
import hu.unideb.inf.rft.neuban.service.exceptions.data.CardNotFoundException;
import hu.unideb.inf.rft.neuban.service.exceptions.data.ColumnNotFoundException;
import hu.unideb.inf.rft.neuban.service.exceptions.data.ColumnNotInSameBoardException;
import hu.unideb.inf.rft.neuban.service.exceptions.data.DataNotFoundException;
import hu.unideb.inf.rft.neuban.service.exceptions.data.ParentBoardNotFoundException;
import hu.unideb.inf.rft.neuban.service.exceptions.data.ParentColumnNotFoundException;
import hu.unideb.inf.rft.neuban.service.interfaces.CardService;
import hu.unideb.inf.rft.neuban.service.interfaces.ColumnService;
import hu.unideb.inf.rft.neuban.service.interfaces.shared.SingleDataGetService;
import hu.unideb.inf.rft.neuban.service.interfaces.shared.SingleDataUpdateService;

@Service
public class CardServiceImpl implements CardService {

	@Autowired
	private ColumnService columnService;
	@Autowired
	private CardService cardService;
	@Autowired
	private CardRepository cardRepository;
	@Autowired
	private BoardRepository boardRepository;
	@Autowired
	private ColumnRepository columnRepository;

	@Autowired
	@Qualifier(SINGLE_CARD_DATA_GET_SERVICE)
	private SingleDataGetService<CardDto, Long> singleCardDataGetService;

	@Autowired
	@Qualifier(SINGLE_CARD_DATA_UPDATE_SERVICE)
	private SingleDataUpdateService<CardDto> singleCardDataUpdateService;

	@Transactional(readOnly = true)
	@Override
	public Optional<CardDto> get(final Long cardId) {
		return this.singleCardDataGetService.get(cardId);
	}

	@Transactional(readOnly = true)
	@Override
	public List<CardDto> getAllByColumnId(final Long columnId) {
		final Optional<ColumnDto> columnDtoOptional = this.columnService.get(columnId);

		if (columnDtoOptional.isPresent()) {
			return columnDtoOptional.get().getCards();
		}
		return Lists.newArrayList();
	}

	@Transactional
	@Override
	public void save(final Long columnId, final CardDto cardDto)
			throws DataNotFoundException, CardAlreadyExistsException {
		Assert.notNull(cardDto);
		final ColumnDto columnDto = this.columnService.get(columnId)
				.orElseThrow(() -> new ColumnNotFoundException(String.valueOf(columnId)));

		if (cardDto.getId() == null
				|| columnDto.getCards().stream().noneMatch(c -> c.getId().equals(cardDto.getId()))) {
			columnDto.getCards().add(cardDto);
		} else {
			throw new CardAlreadyExistsException(String.valueOf(cardDto.getId()));
		}
		this.columnService.update(columnDto);
	}

	@Transactional
	@Override
	public void update(final CardDto cardDto) throws DataNotFoundException {
		this.singleCardDataUpdateService.update(cardDto);
	}

	@Transactional
	@Override
	public void remove(final Long cardId) throws CardNotFoundException {
		Assert.notNull(cardId);

		Optional.ofNullable(this.cardRepository.findOne(cardId))
				.orElseThrow(() -> new CardNotFoundException(String.valueOf(cardId)));

		this.cardRepository.delete(cardId);
	}

	@Transactional
	@Override
	public void moveCardToAnotherColumn(final Long columnId, final Long cardId)
			throws DataNotFoundException, ColumnAlreadyExistsException {

		Assert.notNull(columnId);
		Assert.notNull(cardId);

		final BoardEntity parentBoardEntity = Optional
				.ofNullable(this.boardRepository.findParentBoardbyColumnId(columnId))
				.orElseThrow(ParentBoardNotFoundException::new);

		final ColumnEntity parentColumnEntity = Optional
				.ofNullable(this.columnRepository.findParentColumnByCardId(cardId))
				.orElseThrow(ParentColumnNotFoundException::new);

		final BoardEntity parentBoardEntityOfParentColumnEntityOfCard = Optional
				.ofNullable(this.boardRepository.findParentBoardbyColumnId(parentColumnEntity.getId()))
				.orElseThrow(ParentBoardNotFoundException::new);

		final ColumnDto newColumnDto = this.columnService.get(columnId)
				.orElseThrow(() -> new ColumnNotFoundException(String.valueOf(columnId)));

		final ColumnDto oldColumnDto = this.columnService.get(parentColumnEntity.getId())
				.orElseThrow(() -> new ColumnNotFoundException(String.valueOf(parentColumnEntity.getId())));

		final CardDto cardDto = this.get(cardId).orElseThrow(() -> new CardNotFoundException(String.valueOf(cardId)));

		if (parentBoardEntity.equals(parentBoardEntityOfParentColumnEntityOfCard)) {
			oldColumnDto.getCards().remove(cardDto);
			columnService.update(oldColumnDto);
			cardService.remove(cardDto.getId());
			newColumnDto.getCards().add(cardDto);
			columnService.update(newColumnDto);
		} else {
			throw new ColumnNotInSameBoardException(columnId.toString());
		}
	}
	@Override
	@Transactional
	public void moveCardToAnotherColumnByDirection(final Long cardId, Boolean direction)
			throws ParentColumnNotFoundException, ParentBoardNotFoundException {

		Assert.notNull(cardId);
		Assert.notNull(direction);

		final ColumnEntity parentColumnEntity = Optional
				.ofNullable(this.columnRepository.findParentColumnByCardId(cardId))
				.orElseThrow(ParentColumnNotFoundException::new);

		final BoardEntity parentBoardEntityOfParentColumnEntityOfCard = Optional
				.ofNullable(this.boardRepository.findParentBoardbyColumnId(parentColumnEntity.getId()))
				.orElseThrow(ParentBoardNotFoundException::new);

		final CardEntity cardEntity = cardRepository.findOne(cardId);

		int indexOfCol = parentBoardEntityOfParentColumnEntityOfCard.getColumns().indexOf(parentColumnEntity);
		if (indexOfCol < parentBoardEntityOfParentColumnEntityOfCard.getColumns().size()-1 && direction == true) {
			final ColumnEntity targetColumn = parentBoardEntityOfParentColumnEntityOfCard.getColumns()
					.get(indexOfCol + 1);
			parentColumnEntity.getCards().remove(cardEntity);
			cardRepository.delete(cardEntity);
			columnRepository.saveAndFlush(parentColumnEntity);
			targetColumn.getCards().add(cardEntity);
			columnRepository.saveAndFlush(targetColumn);
		}

		if (indexOfCol > 0 && direction == false) {
			final ColumnEntity targetColumn = parentBoardEntityOfParentColumnEntityOfCard.getColumns()
					.get(indexOfCol - 1);
			parentColumnEntity.getCards().remove(cardEntity);
			cardRepository.delete(cardEntity);
			columnRepository.saveAndFlush(parentColumnEntity);
			targetColumn.getCards().add(cardEntity);
			columnRepository.saveAndFlush(targetColumn);
		}

	}

}

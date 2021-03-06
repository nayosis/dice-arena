package com.serli.dojo.dicearena.stats.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serli.dojo.dicearena.stats.model.Account;
import com.serli.dojo.dicearena.stats.model.Game;
import com.serli.dojo.dicearena.stats.model.Match;
import com.serli.dojo.dicearena.stats.model.Player;
import com.serli.dojo.dicearena.stats.model.Score;
import com.serli.dojo.dicearena.stats.model.Stat;

public class ElasticSearchStatsService implements StatsService {

	public static final String INDEX = "arena-stats";

	private Client client;
	private ObjectMapper mapper = new ObjectMapper().findAndRegisterModules().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	public ElasticSearchStatsService(Client client) {
		this.client = client;
	}

	@Override
	public Account getAccount(String email) {
		return getUniqueTypedStatForQuery(Account.TYPE, QueryBuilders.termQuery("email", email), Account.class);
	}

	@Override
	public Player getPlayer(String name) {
		return getUniqueTypedStatForQuery(Player.TYPE, QueryBuilders.termQuery("name", name), Player.class);
	}

	@Override
	public Game getGame(String name) {
		return getUniqueTypedStatForQuery(Game.TYPE, QueryBuilders.termQuery("name", name), Game.class);
	}

	@Override
	public Match getMatch(String id) {
		return getUniqueTypedStatForQuery(Match.TYPE, QueryBuilders.termQuery("id", id), Match.class);
	}

	public List<Stat> searchStats(String query) {
		return searchStats(query, 0);
	}

	public List<Stat> searchStats(String query, int from) {
		return getStatsForQuery(QueryBuilders.queryString(query), from);
	}

	public List<Stat> getLastStats() {
		return getLastStats(0);
	}

	public List<Stat> getLastStats(int from) {
		return getStatsForQuery(QueryBuilders.matchAllQuery(), from);
	}

	public List<Stat> getStatsForPlayer(Player player) {
		return getStatsForPlayer(player, 0);
	}

	public List<Stat> getStatsForPlayer(Player player, int from) {
		return getTypeSortedStatsForQuery(QueryBuilders.boolQuery()
				.should(QueryBuilders.termQuery("scores.player", player.getName()))
				.should(QueryBuilders.termQuery("name", player.getGame())), from);
	}

	public List<Stat> getStatsForGame(Game game) {
		return getStatsForGame(game, 0);
	}

	public List<Stat> getStatsForGame(Game game, int from) {
		return getStatsForQuery(QueryBuilders.matchQuery("game", game.getName()), from);
	}

	public List<Stat> getStatsForMatch(Match match) {
		return getStatsForMatch(match, 0);
	}

	public List<Stat> getStatsForMatch(Match match, int from) {
		return getTypeSortedStatsForQuery(QueryBuilders.boolQuery()
				.should(QueryBuilders.termQuery("name", match.getGame()))
				.should(QueryBuilders.termsQuery("name", match.getScores().stream().map(Score::getPlayer).collect(Collectors.toList()))), from);
	}

	private SearchRequestBuilder prepareQuery(QueryBuilder query) {
		return client.prepareSearch("arena-stats").setQuery(query);
	}

	private SearchRequestBuilder prepareRequest(SearchRequestBuilder searchRequestBuilder, int from) {
		searchRequestBuilder.addSort("_timestamp", SortOrder.DESC).setSize(6).setFrom(from);
		return searchRequestBuilder;
	}

	private <S extends Stat> S getUniqueTypedStatForQuery(String type, QueryBuilder query, Class<S> statClass) {
		List<Stat> stats = getStatsForRequest(prepareQuery(query));
		if (stats.size() > 0 && statClass.isInstance(stats.get(0))) {
			return statClass.cast(stats.get(0));
		}
		return null;
	}

	private List<Stat> getStatsForQuery(QueryBuilder query, int from) {
		return getStatsForRequest(prepareRequest(prepareQuery(query), from));
	}

	private List<Stat> getTypeSortedStatsForQuery(QueryBuilder query, int from) {
		return getStatsForRequest(prepareRequest(prepareQuery(query).addSort("_type", SortOrder.ASC), from));
	}

	private List<Stat> getStatsForRequest(SearchRequestBuilder request) {
		return Arrays.asList(request.get().getHits().getHits()).stream().map(this::getStatForHit).collect(Collectors.toList());
	}

	private Stat getStatForHit(SearchHit hit) {
		try {
			switch (hit.getType()) {
			case Account.TYPE:
				return mapper.readValue(hit.getSourceAsString(), Account.class);
			case Player.TYPE:
				return mapper.readValue(hit.getSourceAsString(), Player.class);
			case Game.TYPE:
				return mapper.readValue(hit.getSourceAsString(), Game.class);
			case Match.TYPE:
				return mapper.readValue(hit.getSourceAsString(), Match.class);
			default:
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}
}

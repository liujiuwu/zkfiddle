package org.zkoss.fiddle.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.zkoss.fiddle.core.utils.CacheHandler;
import org.zkoss.fiddle.core.utils.FiddleCache;
import org.zkoss.fiddle.dao.api.ITagDao;
import org.zkoss.fiddle.model.Tag;

@SuppressWarnings("unchecked")
public class TagDaoImpl extends AbstractDao implements ITagDao{

	public List<Tag> list() {
		return getHibernateTemplate().find("from Tag");
	}

	public void saveOrUdate(Tag m) {
		super.saveOrUdateObject(m);
	}

	public Tag get(Long id) {
		return (Tag) getHibernateTemplate().get(Tag.class, id);
	}

	public void remove(Tag m) {
		getHibernateTemplate().delete(m);
	}

	public void remove(final Long id) {

		getTxTemplate().execute(new HibernateTransacationCallback<Void>(getHibernateTemplate()) {

			public Void doInHibernate(Session session) throws HibernateException, SQLException {
				session.createQuery("delete from Tag where id = :id").setLong("id", id).executeUpdate();
				return null;
			}
		});
	}


	public Tag getTag(final String name) {
		return getHibernateTemplate().execute(new HibernateCallback<Tag>() {

			public Tag doInHibernate(Session session) throws HibernateException, SQLException {
				Query query = session.createQuery("from Tag where name = :name");
				query.setString("name", name);
				return (Tag) query.uniqueResult();
			}
		});
	}

	public List<Tag> searchTag(final String name,final int amount) {
		return getHibernateTemplate().execute(new HibernateCallback<List<Tag>>() {

			public List<Tag> doInHibernate(Session session) throws HibernateException, SQLException {
				Query query = session.createQuery("from Tag where name like :name");
				query.setString("name", name + "%");
				query.setMaxResults(amount);
				return (List<Tag>) query.list();
			}
		});
	}

	
	public List<Tag> searchTag(final String name) {
		return getHibernateTemplate().execute(new HibernateCallback<List<Tag>>() {

			public List<Tag> doInHibernate(Session session) throws HibernateException, SQLException {
				Query query = session.createQuery("from Tag where name like :name");
				query.setString("name", name + "%");
				return (List<Tag>) query.list();
			}
		});
	}

	/**
	 * 2011/6/26 TonyQ:
	 * This is a time consuming method , please don't count on this too much.lol
	 */
	public List<Tag> prepareTags(final String[] tags) {

		return getTxTemplate().execute(new HibernateTransacationCallback<List<Tag>>(getHibernateTemplate()) {

			public List<Tag> doInHibernate(Session session) throws HibernateException, SQLException {
				Query query = session.createQuery("from Tag where name in (:list) ");
				Set<String> tagSet = new HashSet<String>(Arrays.asList(tags));
				query.setParameterList("list", tagSet);
				List<Tag> result = query.list();

				// a quick result to make it faster if need not to insert.
				if (result.size() == tagSet.size()) {
					return result;
				}
				Map<String, Tag> map = new HashMap<String, Tag>();
				for (Tag t : result) {
					map.put(t.getName(), t);
				}

				List<Tag> list = new ArrayList<Tag>();

				for (String token : tagSet) {

					if (!map.containsKey(token)) {
						Tag t = new Tag();
						t.setName(token);
						t.setAmount(0L);
						session.save(t);
						list.add(t);
					} else {
						list.add(map.get(token));
					}
				}

				return list;

			}
		});
	}

	public List<Tag> findPopularTags(final int amount) {
		return (List<Tag>) FiddleCache.CaseTag.execute(new CacheHandler<List<Tag>>() {
			protected List<Tag> execute() {
				return  _findPopularTags(amount);
			}

			protected String getKey() {
				return "popularTags:" + amount;
			}
		});

	}

	private List<Tag> _findPopularTags(final int amount) {
		return getHibernateTemplate().execute(new HibernateCallback<List<Tag>>() {
			public List<Tag> doInHibernate(Session session) throws HibernateException, SQLException {
				Query query = session.createQuery("from Tag where amount > 0 order by amount desc ");
				query.setMaxResults(amount);
				return query.list();
			}
		});
	}


}

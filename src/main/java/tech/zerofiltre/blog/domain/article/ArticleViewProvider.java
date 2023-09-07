package tech.zerofiltre.blog.domain.article;

import tech.zerofiltre.blog.domain.article.model.*;

import java.util.*;

public interface ArticleViewProvider {

    List<ArticleView> viewsOfArticle(long articleId);

    ArticleView save(ArticleView articleView);

    List<ArticleView> viewsOfUser(long viewerId);

    void delete(ArticleView articleView);
}

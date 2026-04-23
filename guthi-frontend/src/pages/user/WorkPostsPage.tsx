import { useEffect, useState } from "react";
import { apiJson } from "../../api/client";
import { LIVE_TOPICS } from "../../live/topics";
import { useStompSubscription } from "../../live/StompContext";

type Post = {
  id: string;
  posterName: string;
  title: string;
  description: string | null;
  mobileNumber: string | null;
  postedAt: string;
  acceptanceCount: number;
  acceptedByNames: string[];
};

export function WorkPostsPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [mobile, setMobile] = useState("");
  const [err, setErr] = useState("");
  const [msg, setMsg] = useState("");

  async function load() {
    const data = await apiJson<Post[]>("/api/work-posts");
    setPosts(data);
  }

  useEffect(() => {
    load().catch((e) => setErr(e instanceof Error ? e.message : "Load failed"));
  }, []);

  useStompSubscription(LIVE_TOPICS.workPosts, (body) => {
    try {
      setPosts(JSON.parse(body) as Post[]);
    } catch {
      /* ignore */
    }
  });

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setErr("");
    setMsg("");
    try {
      await apiJson("/api/work-posts", {
        method: "POST",
        body: JSON.stringify({ title, description: description || undefined, mobileNumber: mobile }),
      });
      setMsg("Post created.");
      setTitle("");
      setDescription("");
      setMobile("");
      await load();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Create failed");
    }
  }

  async function accept(post: Post) {
    setErr("");
    setMsg("");
    try {
      await apiJson(`/api/work-posts/${post.id}/accept`, { method: "POST" });
      setMsg(`Accepted "${post.title}".`);
      await load();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Accept failed");
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">Work posts</h1>
        <p className="page__lede">Offer help or post opportunities your neighbours can accept.</p>
      </header>
      {err ? <p className="error">{err}</p> : null}
      {msg ? <p className="success">{msg}</p> : null}

      <div className="panel">
        <h2 className="panel__title">New post</h2>
        <form className="form-stack" onSubmit={create}>
          <label htmlFor="work-title">Title</label>
          <input id="work-title" value={title} onChange={(e) => setTitle(e.target.value)} required />
          <label htmlFor="work-mobile">Mobile number</label>
          <input
            id="work-mobile"
            type="tel"
            placeholder="e.g. 9801122334 or +9779801122334"
            value={mobile}
            onChange={(e) => setMobile(e.target.value)}
            pattern="^[+]?[0-9]{7,15}$"
            title="7–15 digits, optional leading +"
            required
          />
          <label htmlFor="work-desc">Description</label>
          <textarea id="work-desc" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
          <button type="submit">Post</button>
        </form>
      </div>

      <h2 className="page__section-title">All posts</h2>
      {posts.map((p) => (
        <div key={p.id} className="panel">
          <h3 style={{ marginTop: 0, marginBottom: "0.35rem" }}>{p.title}</h3>
          <p className="empty-hint">By {p.posterName}</p>
          {p.mobileNumber ? (
            <p className="event-meta">📞 <a href={`tel:${p.mobileNumber}`}>{p.mobileNumber}</a></p>
          ) : null}
          {p.description ? <p>{p.description}</p> : null}
          <p className="event-meta">
            <strong>Accepted:</strong> {p.acceptanceCount}{" "}
            {p.acceptedByNames.length ? `(${p.acceptedByNames.join(", ")})` : ""}
          </p>
          <button type="button" className="secondary" onClick={() => accept(p)}>
            Accept / help
          </button>
        </div>
      ))}
    </div>
  );
}
